import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import xyz.jpenilla.runpaper.task.RunServer
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit

plugins {
    java
    alias(libs.plugins.shadow)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.paperweight.userdev)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.run.paper)
}

// "26.2.build.124-stable" is two facts in one string: the Minecraft version the plugin targets and the
// Paper build the dev bundle comes from. `api-version` in plugin.yml has to be the first of them, and
// so does the server the test boots, so both read it from here rather than repeating it.
val paperVersion: String = libs.versions.paper.get()
val paperMinecraftVersion: String = paperVersion.substringBefore(".build.")
val shadePrefix: String by project

repositories {
    // In front of Central on purpose: Central answers some requests from some networks with 403
    // ("Forbidden"), and a refused request ends resolution for that repository instead of falling
    // through to the next one, so the mirror has to come first. Enable it with `-PcnMirror` or by
    // adding `cnMirror=true` to <GRADLE_USER_HOME>/gradle.properties; that is a machine setting rather
    // than a project one because a CI runner does not need it.
    if (providers.gradleProperty("cnMirror").isPresent) {
        maven("https://maven.aliyun.com/repository/public")
    }
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.skriptlang.org/releases")
    maven("https://repo.destroystokyo.com/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
}

dependencies {
    implementation(kotlin("stdlib"))
    // Server internals (net.minecraft.*, org.bukkit.craftbukkit.*) are not on any public repository:
    // paperweight is the only supported way to compile against them. Paper 26.x runs Mojang-mapped,
    // so there is no reobfuscation step and the compiled output loads as-is.
    paperweight.paperDevBundle(paperVersion)
    compileOnly(libs.skript)
    compileOnly(libs.packetevents)

    testImplementation(kotlin("test"))
    // Stated rather than inherited: MockBukkit does not put the API this project compiles against on
    // the test classpath, and the tests need Bukkit and Adventure types to mock and to build titles.
    testImplementation(libs.paper.api)
    testImplementation(libs.mockbukkit)
    testImplementation(libs.mockk)
}

paperweight {
    addServerDependencyTo = configurations.named(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME).map { setOf(it) }
}

java {
    withSourcesJar()
    // Paper 26.2 is compiled for Java 25, so the compiler has to be able to read version 69 class
    // files. Gradle provisions or locates the toolchain; the server itself already requires Java 25.
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.addAll("-Xallow-unstable-dependencies")
    }
}

tasks {
    base {
        archivesName.set("xiaojiegui")
    }

    // `plugin.yml` is the one place a server reads the version from, so it is expanded from the same
    // value the jar is named with rather than repeating the literal.
    processResources {
        val expansions = mapOf(
            "version" to version.toString(),
            "apiVersion" to paperMinecraftVersion
        )
        inputs.properties(expansions)
        filesMatching("plugin.yml") {
            expand(expansions)
        }
    }

    withType<ShadowJar> {
        val kotlinEscapedVersion = libs.versions.kotlin.get().filter { it != '.' }
        archiveAppendix.set("")
        archiveClassifier.set("")
        archiveVersion.set(version.toString())
        destinationDirectory.set(file("$rootDir/build/dist"))

        minimize()

        // The server classpath is shared with every other plugin, so anything shaded in is relocated
        // into a package of its own.
        relocate("kotlin.", "$shadePrefix.kotlin$kotlinEscapedVersion.")
        relocate("org.jetbrains.annotations.", "$shadePrefix.org.jetbrains.annotations2602.")
    }

    build {
        dependsOn(shadowJar)
    }

    test {
        useJUnitPlatform()
        // MockK instruments classes by attaching a ByteBuddy agent to the JVM it runs in. JDK 25
        // refuses self-attach unless this is set, and the external-process fallback needs a JVM the
        // Gradle worker cannot always spawn, so without it every mocking test dies in its initializer.
        jvmArgs("-Djdk.attach.allowAttachSelf=true")
    }
}

// --- release notes ----------------------------------------------------------------------------------
//
// Writes the CHANGELOG section for the version in `gradle.properties` to `build/release-notes.md`, which
// is what the release workflow publishes. The release page and the file therefore cannot disagree, and
// correcting the notes of a release that is already published is the same extraction plus
// `gh release edit v<version> --notes-file build/release-notes.md`.

val releaseNotes by tasks.registering {
    group = "documentation"
    description = "Extracts this version's CHANGELOG section into build/release-notes.md."

    val changelog = layout.projectDirectory.file("CHANGELOG.md")
    val releaseVersion = version.toString()
    val releaseNotesFile = layout.buildDirectory.file("release-notes.md")
    inputs.file(changelog)
    inputs.property("version", releaseVersion)
    outputs.file(releaseNotesFile)

    doLast {
        val heading = "## $releaseVersion"
        val lines = changelog.asFile.readLines()
        val start = lines.indexOfFirst { it.trim() == heading }
        if (start < 0) {
            throw GradleException(
                "CHANGELOG.md has no '$heading' section. Write what this release changes before releasing."
            )
        }
        // Up to the next version heading, so one release cannot publish another one's notes.
        val rest = lines.drop(start + 1)
        val next = rest.indexOfFirst { it.startsWith("## ") }
        val section = (if (next < 0) rest else rest.take(next)).joinToString("\n").trim()
        if (section.isEmpty()) {
            throw GradleException("The '$heading' section of CHANGELOG.md is empty.")
        }
        val target = releaseNotesFile.get().asFile
        target.parentFile.mkdirs()
        target.writeText(section + "\n")
        logger.lifecycle("Wrote $target (${section.lines().size} lines) from '$heading'.")
    }
}

// --- server test ------------------------------------------------------------------------------------
//
// Boots a real Paper server with Skript next to the built jar and reads the log afterwards. It is the
// only layer that can see registration and parse failures: Skript reports those in prose and carries
// on, and MockBukkit cannot host Skript at all (Skript's main class is final, and MockBukkit loads a
// plugin by subclassing it).
//
// `server-test/skript/*.sk` reports what it did as `XIAOJIE_SELFTEST detail: <name> -> <message>`
// lines; the assertions live in `VerifySkriptServerTest` below, next to the log they read.

val serverTestDirectory = layout.projectDirectory.dir("server-test/run")
val serverTestScripts = layout.projectDirectory.dir("server-test/skript")
val serverTestElements =
    layout.projectDirectory.dir("src/main/kotlin/io/github/heyhey123/xiaojiegui/skript/elements")

// The plugins the server needs besides ours, pinned to the releases that support Paper 26.2: Skript
// 2.16.2 is the release that added 26.2, PacketEvents 2.13.0 is the one that did too, and SkBee 3.25.4
// is the release whose changelog names 26.2 and Skript 2.15+ as its floor; it is in this list because
// it is the only thing that can hand the addon a text component, which is what the title path needs.
// Downloaded by this build rather than by run-paper so the exact asset is visible next to that reason;
// SkBee publishes no asset on GitHub, so its URL is the Modrinth CDN file of that release.
val serverTestPlugins = mapOf(
    "Skript-2.16.2.jar" to
        "https://github.com/SkriptLang/Skript/releases/download/2.16.2/Skript-2.16.2.jar",
    "packetevents-spigot-2.13.0.jar" to
        "https://github.com/retrooper/packetevents/releases/download/v2.13.0/packetevents-spigot-2.13.0.jar",
    "SkBee-3.25.4.jar" to
        "https://cdn.modrinth.com/data/a0tlbHZO/versions/bTBlzhGZ/SkBee-3.25.4.jar"
)

// One entry per `XIAOJIE_SELFTEST detail:` name the scripts report, mapped to a substring its message
// has to contain. Kept here rather than derived from the scripts so that a script whose element stopped
// working fails the test instead of quietly shrinking what is covered; the substring is what makes the
// line an assertion, since a line that exists but reports the wrong count, mode or title now fails.
//
// The values are the ones the run this file was written against reported. An empty value means the
// message cannot be pinned, and every one of those carries the reason next to it.
val serverTestExpectedDetails = mapOf(
    "create phantom chest menu" to "1 page(s)",
    "lookup menu by id" to "id selftest, mode phantom, type chest inventory",
    "menu mode is text" to "phantom",
    "menu defaults" to "page 1, delay 50, 0 viewer(s)",
    "enumerate menus" to "1 id(s), 1 menu(s)",
    "player inventory condition" to "shown",
    "destroy menu" to "destroyed",
    "create static hopper menu" to "1 page(s), mode static",
    "create menu hiding the player inventory" to "default title HideSelftest, type chest inventory",
    // "not hidden" contains "hidden", so this pins the flag but the script keeps its own FAILED branch.
    "hide flag took effect" to "hidden",
    // One file per inventory type: that type's layout becoming a page is the whole assertion.
    "inventory type beacon" to "1 page(s)",
    "inventory type workbench" to "1 page(s)",
    "inventory type crafter" to "1 page(s)",
    "inventory type lectern" to "1 page(s)",
    "inventory type smithing" to "1 page(s)",
    "inventory type barrel" to "1 page(s)",
    "inventory type dispenser" to "1 page(s)",
    "inventory type ender chest" to "1 page(s)",
    "inventory type enchanting" to "1 page(s)",
    "inventory type cartography" to "1 page(s)",
    "create pages menu" to "default title Pages, type chest inventory",
    "insert pages" to "3 page(s)",
    "create keys menu" to "default title Keys, type chest inventory",
    "keys and overrides" to "9 slot(s), key A",
    "page title" to "Page One",
    // The callback body can only run for a clicking player and this layer has none, so the line reports
    // that the section parsed and registered; there is no runtime value to pin.
    "slot callback section" to "registered",
    "destroy both menus" to "done",
    "create titles menu" to "default title Title Menu, type chest inventory",
    "insert page with title" to "2 page(s), page 2 titled Second",
    "update page title" to "Renamed",
    "tagged title form changes nothing" to "Renamed",
    // Both statements need a player and a session this layer does not have, so `parsed` is the value.
    "turn page title parses" to "parsed",
    "session title parses" to "parsed",
    "destroy titles menu" to "done",
    "create forms menu" to "default title Forms, type chest inventory",
    "readme example menu" to "1 page(s), key A at slot 12",
    "natural menu forms" to "9 slot(s), key A",
    "destroy menu by id" to "done",
    "create paged menu" to "default title Paged, type chest inventory",
    "page contract" to "the layout became page 1",
    "page contract default page" to "2",
    // Only reachable with SkBee installed: a title written as a text component, read back as text.
    "skbee component title" to "Selftest Component Title"
)

// Names a script reports that the map above does not mention, so adding a name to a script is a test
// failure rather than a line nothing checks.
//
// A name containing `FAILED` is the other half of that mechanism and is expected to be undeclared: the
// scripts log it only from the branch that found the wrong thing, so a run in which it appears fails
// here instead of passing on a line that merely exists.
//
// `11-client.sk` is left out of this check: it reports its lines only while a real client is connected,
// which this run never has, and those names are declared in `clientTestExpectedDetails` below instead.
// `clientTest` reads the same file for its own version of this check, so the names stay covered.
val serverTestUndeclaredDetails = providers.provider {
    val clientScript = serverTestScripts.file("11-client.sk").asFile
    val reported = serverTestScripts.asFile.walkTopDown()
        .filter { it.isFile && it.extension == "sk" && it != clientScript }
        .flatMap { file ->
            // Comments describe the line format, so they would otherwise be read as names nothing declares.
            file.readLines()
                .filterNot { it.trimStart().startsWith("#") }
                .flatMap { line ->
                    Regex("""XIAOJIE_SELFTEST detail:\s*(.+?)\s*->""")
                        .findAll(line)
                        .map { it.groupValues[1] }
                }
                .asSequence()
        }
        .filterNot { "FAILED" in it }
        .toSet()
    reported - serverTestExpectedDetails.keys
}

val prepareServerTest by tasks.registering {
    description = "Prepares the disposable server directory that `runServer` and `serverTest` use."
    group = "verification"
    inputs.dir(serverTestScripts)
    inputs.dir(serverTestElements)
    inputs.file("server-test/server.properties")
    doLast {
        val run = serverTestDirectory.asFile
        val scripts = run.resolve("plugins/Skript/scripts")
        scripts.mkdirs()
        // Paper refuses to start without this. Writing it records acceptance of the Minecraft EULA
        // (https://aka.ms/MinecraftEULA) for this disposable test server, and for no other server.
        run.resolve("eula.txt").writeText("eula=true\n")
        file("server-test/server.properties").copyTo(run.resolve("server.properties"), overwrite = true)
        scripts.deleteRecursively()
        scripts.mkdirs()
        serverTestScripts.asFile.copyRecursively(scripts, overwrite = true)
        // The one script that is not checked in: it is rebuilt from the elements' own `@Example`
        // annotations, so a documented example that Skript cannot read fails the run like any other
        // broken line instead of living only in the docs.
        scripts.resolve("07-examples.sk").writeText(exampleGateScript(serverTestElements.asFile))
        serverTestPlugins.forEach { (name, url) ->
            val jar = run.resolve("plugins/$name")
            if (jar.isFile) return@forEach
            logger.lifecycle("Downloading $name")
            URI(url).toURL().openStream().use { input -> jar.outputStream().use { input.copyTo(it) } }
        }
    }
}

tasks.named<RunServer>("runServer") {
    dependsOn(prepareServerTest)
    minecraftVersion(paperMinecraftVersion)
    runDirectory.set(serverTestDirectory)
    // The shaded jar is the plugin under test; run-paper copies it into the run directory on each run.
    pluginJars(tasks.shadowJar)
    // Paper 26.2 refuses to start on anything older than Java 25, and the build itself may be running
    // on an older JDK, so the server starts on the toolchain the code is compiled for.
    javaLauncher.set(javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(25) })
}

val serverTest by tasks.registering(VerifySkriptServerTest::class) {
    description = "Boots a Paper server with Skript and checks what this plugin did there."
    group = "verification"
    dependsOn(tasks.named("runServer"))
    serverLog.set(serverTestDirectory.file("logs/latest.log"))
    expectedDetails.set(serverTestExpectedDetails)
    undeclaredDetails.set(serverTestUndeclaredDetails)
}

/**
 * Checks the log a `serverTest` run wrote.
 *
 * The Skript side reports what it did as `XIAOJIE_SELFTEST` lines, so the assertions stay here, where
 * they can be read and changed without writing Skript.
 */
abstract class VerifySkriptServerTest : DefaultTask() {

    /** The log of the server that just ran. */
    @get:Internal
    abstract val serverLog: RegularFileProperty

    /** For each `XIAOJIE_SELFTEST detail:` name the scripts have to report, a substring its message must contain. */
    @get:Input
    abstract val expectedDetails: MapProperty<String, String>

    /** Names a script reports that [expectedDetails] does not list. */
    @get:Input
    abstract val undeclaredDetails: SetProperty<String>

    @TaskAction
    fun checkLog() {
        val log = serverLog.get().asFile
        if (!log.isFile) throw GradleException("The server wrote no log at ${log.absolutePath}.")

        val lines = log.readLines()
        val problems = mutableListOf<String>()

        fun requireLine(description: String, text: String) {
            if (lines.none { text in it }) problems += "$description\n      absent: $text"
        }

        fun forbidLine(description: String, pattern: Regex) {
            lines.firstOrNull { pattern.containsMatchIn(it) }
                ?.let { problems += "$description\n      found: $it" }
        }

        requireLine("The plugin never announced that it enabled.", "XiaojieGUI has been enabled!")
        requireLine("The self-test stopped early: it never reached its last line.", "XIAOJIE_SELFTEST=STOPPED")
        // Two different failures, reported as two different things: a name nothing logged is a missing
        // check, and a name that logged the wrong message is a check that ran and found something else.
        expectedDetails.get().forEach { (detail, expected) ->
            val marker = "XIAOJIE_SELFTEST detail: $detail ->"
            val line = lines.firstOrNull { marker in it }
            if (line == null) {
                problems += "The script never reported `$detail`.\n      absent: $marker"
                return@forEach
            }
            val message = line.substringAfter(marker)
            if (expected.isNotEmpty() && expected !in message) {
                problems += "The script reported `$detail`, but with the wrong message." +
                    "\n      present:  ${message.trim()}" +
                    "\n      expected: a message containing `$expected`"
            }
        }

        // Skript reports a broken element or an unparseable script in prose and keeps going, so the log
        // is the only place such a failure shows up. Every shape of that message is covered: a line Skript
        // cannot read says "this line", a statement says "this condition/effect" and a condition says
        // "this condition", so matching the prefix catches all three.
        forbidLine("Skript reported a severe error.", Regex("""\[Skript]\s+Severe Error"""))
        forbidLine("Skript could not compile a registered pattern.", Regex("pattern compiling exception"))
        forbidLine("A script line could not be understood.", Regex("""Can't understand this"""))
        // Skript refuses an expression that belongs to another event with "The expression 'x' may only be
        // used in an inventory click event", which is not one of the three wordings above. The `[Skript]
        // Line N:` header cannot be forbidden itself: warnings and the addon's own deliberate runtime
        // errors (`Title cannot be null.`) print under the same header.
        forbidLine(
            "An expression was used outside the event it belongs to.",
            Regex("""may only be used in an? [a-z ]*event""")
        )
        // Skript also refuses an expression whose value has no such property ("The expression 'viewers of
        // {_menu}' returns the following types that do not have the size property: player ..."), which is
        // a parse error like the ones above. The list grows as wordings turn up: every one of these was
        // found by a script that broke, not by reading Skript's source.
        forbidLine(
            "An expression was asked for a property its value does not have.",
            Regex("""returns the following types that do not have the \w+ property""")
        )
        // The scripts name a branch `... FAILED ...` when it found the wrong thing, and that half of the
        // mechanism only works here: the undeclared-name check below reads the scripts, not the log.
        forbidLine("One of the scripts reported a failed check.", Regex("""XIAOJIE_SELFTEST detail: .*FAILED"""))

        val undeclared = undeclaredDetails.get()
        if (undeclared.isNotEmpty()) {
            problems += "Scripts report names the test does not expect: ${undeclared.joinToString(", ")}"
        }

        if (problems.isNotEmpty()) {
            throw GradleException(
                buildString {
                    append("The server test found ${problems.size} problem(s) in ${log.absolutePath}:")
                    problems.forEach { append("\n  - ").append(it) }
                }
            )
        }
        logger.lifecycle(
            "Server test passed: all ${expectedDetails.get().size} detail line(s) and the shutdown marker are in the log."
        )
    }
}

/**
 * A Skript file that parses every `@Example`/`@Examples` annotation of the elements under [root].
 *
 * The annotations are Skript that users copy, and Skript's docs generator accepts an example its own
 * parser rejects, so nothing checked them. Handing them back to Skript turns a broken example into
 * `Can't understand this` in the log the server test already reads.
 *
 * The example itself decides how it is run: an example is a whole block when its first line starts a
 * top-level script structure, which for this addon is an `on ...:` event or a `command /...:`
 * declaration; that cannot live inside a trigger, so it is written out as it stands. Everything else is
 * a statement, and a statement cannot sit at the top level of a script, so it goes into a command
 * trigger of its own, one command per example. Annotations are already indented the way Skript needs
 * them, so the block form is passed through unchanged rather than re-indented here.
 */
fun exampleGateScript(root: File): String = buildString {
    appendLine("# GENERATED by `prepareServerTest` from the `@Example` annotations under")
    appendLine("# ${root.relativeTo(projectDir).invariantSeparatorsPath}. Do not edit: the annotations are the source.")
    var number = 0
    root.walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .sortedBy { it.relativeTo(root).invariantSeparatorsPath }
        .forEach { file ->
            documentedExamples(file.readText()).forEach { example ->
                number++
                // Tabs are indentation, and one level of it is four spaces everywhere else in this repo.
                val lines = example.lines().map { it.replace("\t", "    ") }
                appendLine()
                appendLine("# ${file.relativeTo(root).invariantSeparatorsPath}")
                if (isWholeBlock(lines)) {
                    lines.forEach { appendLine(it.trimEnd()) }
                } else {
                    appendLine("command /xiaojiegui-example-$number:")
                    appendLine("    trigger:")
                    lines.forEach { appendLine("        ${it.trimEnd()}") }
                }
            }
        }
    if (number == 0) {
        throw GradleException("No `@Example` annotation was found under ${root.absolutePath}.")
    }
    appendLine()
}

/** Whether [lines] start a top-level script structure rather than a statement. */
fun isWholeBlock(lines: List<String>): Boolean {
    val first = lines.firstOrNull { it.isNotBlank() }?.trim() ?: return false
    // A trailing `#` comment is not part of the structure: Skript strips it before it parses the line.
    val head = first.substringBefore('#').trimEnd()
    val keyword = head.substringBefore(' ').substringBefore(':')
    return head.endsWith(":") && keyword in setOf("on", "command")
}

/**
 * The examples [source] documents, one entry per `@Example`/`@Examples` annotation, with the
 * annotation's arguments joined by newlines the way the docs tool shows them.
 *
 * The annotations are read out of the source text because they are a documentation contract: nothing
 * checks that the compiled classes still carry them, so depending on them here would be a second thing
 * to keep working rather than the same thing.
 */
fun documentedExamples(source: String): List<String> {
    val examples = mutableListOf<String>()
    var cursor = 0
    while (true) {
        val annotation = source.indexOf("@Example", cursor)
        if (annotation < 0) return examples
        var open = annotation + "@Example".length
        if (source.getOrNull(open) == 's') open++
        if (source.getOrNull(open) != '(') {
            cursor = annotation + 1
            continue
        }

        val arguments = mutableListOf<StringBuilder>()
        // Kotlin folds `"a" + "b"` in an annotation into the one string the docs tool shows, so the
        // literal after a `+` continues the previous line instead of starting a new one.
        var continued = false
        var depth = 0
        var index = open
        while (index < source.length) {
            val char = source[index]
            when {
                char == '(' -> {
                    depth++
                    index++
                }

                char == ')' -> {
                    depth--
                    index++
                    if (depth == 0) break
                }

                char == '"' -> {
                    // Kotlin's raw strings keep the newline after the opening quotes, and the docs tool
                    // drops it, so it is dropped here too: the examples on both sides stay identical.
                    val raw = source.startsWith("\"\"\"", index)
                    val delimiter = if (raw) "\"\"\"" else "\""
                    val text = StringBuilder()
                    index += delimiter.length
                    while (index < source.length && !source.startsWith(delimiter, index)) {
                        val current = source[index]
                        if (!raw && current == '\\') {
                            index++
                            text.append(
                                when (val escaped = source[index]) {
                                    'n' -> '\n'
                                    't' -> '\t'
                                    'r' -> '\r'
                                    else -> escaped
                                }
                            )
                        } else {
                            text.append(current)
                        }
                        index++
                    }
                    index += delimiter.length

                    if (continued && arguments.isNotEmpty()) {
                        arguments.last().append(text)
                    } else {
                        arguments += StringBuilder(text)
                    }

                    var lookahead = index
                    while (lookahead < source.length && source[lookahead].isWhitespace()) lookahead++
                    continued = source.getOrNull(lookahead) == '+'
                    if (continued) index = lookahead + 1
                }

                else -> index++
            }
        }

        examples += arguments.joinToString("\n") { it.toString() }.removePrefix("\n").trimEnd()
        cursor = index
    }
}

// --- client test ------------------------------------------------------------------------------------
//
// The same server `serverTest` boots, with a real Minecraft client in front of it: the bot in
// `server-test/client` joins, is shown the menu `11-client.sk` opens for it, clicks it, and says what it
// saw; this task then reads the server's own log for what those clicks made the server do. One scenario,
// two witnesses -- the client can see the window it was given and cannot see what the server did with a
// click, and the server can see the click and cannot see the window.
//
// Why this task starts a server itself while `serverTest` does not: `serverTest` runs the server in the
// foreground through run-paper and lets `99-finish.sk` stop it from the running script, which is all a
// scenario without a player needs -- the run ends when the script stops the server. A client needs the
// opposite arrangement, because the bot only has something to talk to while the server is up: the server
// has to be a background process of this task, and this task, not a script, has to stop it afterwards.

val clientTestWork = layout.buildDirectory.dir("client-test")
val clientBotDirectory = layout.projectDirectory.dir("server-test/client")

// One entry per `XIAOJIE_SELFTEST detail:` name `11-client.sk` reports, mapped to a substring its message
// has to contain. The same shape as `serverTestExpectedDetails` above and for the same reason: a line
// that exists but reports another slot or another click type fails here, so the values are the ones the
// run this file was written against reported.
val clientTestExpectedDetails = mapOf(
    // The addon's slots, from the addon's side: key A is slot 0, which is the slot the client has to hold
    // it in and the slot the bot clicks. The addon and the protocol number a menu's slots the same way,
    // and this is the line that says so for the run the bot is reading.
    "client menu layout" to "A at 0, B at 1, C at 2, N at 3",
    "client menu opened" to "title Client Page One, page 1 of 2",
    // One line per click, in this order: the plain left click, the shift click, number key 3, and the
    // click that turns the page -- each one the click type the bot asked for (`<none>` is an unset key).
    // The page is 1-based, like every other page number a script sees, and the menu counts the one viewer
    // who is clicking it.
    "client plain click" to "slot 0 on page 1, type left mouse button, key <none>, 1 viewer(s)",
    "client shift click" to "slot 1 on page 1, type left mouse button with shift, key <none>, 1 viewer(s)",
    "client number key click" to "slot 2 on page 1, type number key, key 3, 1 viewer(s)",
    "client page 2 button" to "slot 3 on page 1, type left mouse button, key <none>, 1 viewer(s)",
    "client page turned" to "1 -> 2"
)

// The names `11-client.sk` reports that the map above does not declare. `serverTestUndeclaredDetails`
// leaves that file out (its lines need a player), so this is where those names are held to the same
// rule: a line in the client scenario that nothing declares fails the build.
val clientTestUndeclaredDetails = providers.provider {
    val reported = serverTestScripts.file("11-client.sk").asFile.readLines()
        .filterNot { it.trimStart().startsWith("#") }
        .flatMap { line ->
            Regex("""XIAOJIE_SELFTEST detail:\s*(.+?)\s*->""")
                .findAll(line)
                .map { it.groupValues[1] }
        }
        .filterNot { "FAILED" in it }
        .toSet()
    reported - clientTestExpectedDetails.keys
}

val clientTest by tasks.registering(ClientTest::class) {
    description = "Boots a Paper server, has a real client click the menu this plugin opens for it, and checks both sides."
    group = "verification"
    // The same run directory and the same plugin jar `serverTest` uses, so both layers test one artifact.
    dependsOn(prepareServerTest, tasks.named("shadowJar"))
    serverDirectory.set(serverTestDirectory)
    minecraftVersion.set(paperMinecraftVersion)
    pluginJar.set(tasks.named<ShadowJar>("shadowJar").flatMap { it.archiveFile })
    botDirectory.set(clientBotDirectory)
    javaExecutable.set(
        javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(25) }
            .map { it.executablePath.asFile.absolutePath }
    )
    serverLog.set(clientTestWork.map { it.file("server.log") })
    botLog.set(clientTestWork.map { it.file("bot.log") })
    npmCache.set(clientTestWork.map { it.dir("npm-cache") })
    expectedDetails.set(clientTestExpectedDetails)
    undeclaredDetails.set(clientTestUndeclaredDetails)
    // A task that boots a server and a client is never up to date: reporting success because nothing it
    // reads has changed would mean reporting success without having run anything.
    outputs.upToDateWhen { false }
}

/**
 * Runs one client scenario: the bot's dependencies, then a prepared Paper server as a background process,
 * then the bot against it, then the server's log, and stops the server whatever happened.
 */
abstract class ClientTest : DefaultTask() {

    /** The disposable server directory `prepareServerTest` and run-paper share. */
    @get:Internal
    abstract val serverDirectory: DirectoryProperty

    /** The Minecraft version the server and the run directory are for. */
    @get:Input
    abstract val minecraftVersion: Property<String>

    /** The shaded jar, which is the plugin under test. */
    @get:Internal
    abstract val pluginJar: RegularFileProperty

    /** The Node project the bot lives in; its `node_modules` is installed here when it is missing. */
    @get:Internal
    abstract val botDirectory: DirectoryProperty

    /** The Java the server runs on, which Paper 26.2 requires to be 25. */
    @get:Input
    abstract val javaExecutable: Property<String>

    /** Where this task writes the server's own console output for the run. */
    @get:Internal
    abstract val serverLog: RegularFileProperty

    /** Where this task writes the bot's output for the run. */
    @get:Internal
    abstract val botLog: RegularFileProperty

    /** Where npm may keep its cache while it installs the bot's dependencies. */
    @get:Internal
    abstract val npmCache: DirectoryProperty

    /** For each detail line the scenario has to report, a substring its message must contain. */
    @get:Input
    abstract val expectedDetails: MapProperty<String, String>

    /** Names `11-client.sk` reports that [expectedDetails] does not list. */
    @get:Input
    abstract val undeclaredDetails: SetProperty<String>

    @TaskAction
    fun runClientScenario() {
        val dir = serverDirectory.get().asFile
        val version = minecraftVersion.get()
        val paperJar = dir.resolve("versions/$version/paper-$version.jar")
        if (!paperJar.isFile) {
            throw GradleException(
                "The prepared server is missing $paperJar. `runServer` (and so `serverTest`) downloads Paper " +
                    "into the run directory; run `./gradlew serverTest` once, then this task again."
            )
        }

        val changes = prepareRunDirectory(dir, paperJar)
        // The bot's dependencies are installed before the server starts, not between the server being
        // ready and the bot connecting: `99-finish.sk` stops a server nobody is playing on ten seconds
        // after it is up, so an install long enough to matter (a cold one downloads ninety packages) has
        // to happen while there is no server to lose.
        installBotDependencies()
        val log = serverLog.get().asFile
        // A log of this run alone: the file is created empty, so a line can only come from the process
        // this task starts, and not from the run `serverTest` did before it.
        log.parentFile.mkdirs()
        log.delete()

        val process = ProcessBuilder(
            javaExecutable.get(),
            "-Xms1G",
            "-Xmx1G",
            "-cp",
            paperClasspath(dir, paperJar),
            "org.bukkit.craftbukkit.Main",
            "--nogui"
        )
            // The run directory is what makes the server find its own `server.properties`, plugins, world
            // and Skript scripts; the logs are written by this process, so nothing else is competing for
            // the console.
            .directory(dir)
            .redirectErrorStream(true)
            .redirectOutput(log)
            .start()

        var botOutput = ""
        try {
            awaitServerReady(process, log)
            botOutput = runBot()
            checkServerLog(log, botOutput)
        } finally {
            stopServer(process)
            cleanUpRunDirectory(changes)
        }
    }

    /** What [prepareRunDirectory] changed in the run directory, so [cleanUpRunDirectory] can undo it. */
    private data class RunDirectoryChanges(
        /** The generated examples script that was left out, with the bytes it held. */
        val hiddenExamples: Pair<File, ByteArray>?,
        /** The plugin jar this task copied into `plugins/`. */
        val copiedPluginJar: File
    )

    /**
     * Makes the run directory this task's own copy of the prepared one, and reports what it changed.
     *
     * run-paper hands the shaded jar to the server as a plugin path of its own; this task starts the
     * server the way Paperclip re-launches it, from the run directory, where Paper finds plugins in
     * `plugins/`. A jar left there by an earlier run would make Paper report one plugin name for two
     * files and skip the plugin, so the directory is left holding exactly the jar under test.
     */
    private fun prepareRunDirectory(dir: File, paperJar: File): RunDirectoryChanges {
        val plugins = dir.resolve("plugins")
        plugins.listFiles { file -> file.isFile && file.name.startsWith("xiaojiegui") }?.forEach { it.delete() }
        val jar = pluginJar.get().asFile
        val copied = plugins.resolve(jar.name)
        jar.copyTo(copied, overwrite = true)

        // `07-examples.sk` is the one script `prepareServerTest` generates, from the `@Example`
        // annotations, and several of those examples are whole `on menu interact:` / `on page turn:`
        // structures -- real listeners. The interact example turns every menu to page 2 and the page turn
        // example retitles the page, so both would fight this scenario over what the client is shown.
        // `serverTest` keeps covering the examples (it parses them, and has no player to fire them); this
        // run leaves the generated copy out.
        val examples = dir.resolve("plugins/Skript/scripts/07-examples.sk")
        val hidden = if (examples.isFile) examples to examples.readBytes() else null
        examples.delete()
        return RunDirectoryChanges(hidden, copied)
    }

    /**
     * Puts the run directory back the way `prepareServerTest` leaves it.
     *
     * The copied plugin jar goes, because run-paper supplies the same jar itself and two files claiming
     * one plugin name is the `Ambiguous plugin name` error the next `serverTest` would otherwise boot
     * with. The generated examples script comes back, because Gradle is free to run this task before
     * `serverTest` in the same build and that run is where the examples are parsed.
     */
    private fun cleanUpRunDirectory(changes: RunDirectoryChanges) {
        changes.copiedPluginJar.delete()
        changes.hiddenExamples?.let { (file, content) -> file.writeBytes(content) }
    }

    /**
     * The classpath Paperclip re-launches the patched server jar with: the jar first, then every library
     * it extracted next to it.
     *
     * The order matters. `libraries/` holds an older `com.mojang:logging` than the patched jar carries,
     * and with the libraries first the server dies in `LogUtils.getClassLogger()` before it opens a port.
     */
    private fun paperClasspath(dir: File, paperJar: File): String = buildList {
        add(paperJar.absolutePath)
        dir.resolve("libraries").walkTopDown()
            .filter { it.isFile && it.extension == "jar" }
            .forEach { add(it.absolutePath) }
    }.joinToString(File.pathSeparator)

    /** Waits for the server to finish starting, or fails with what the log holds instead. */
    private fun awaitServerReady(process: Process, log: File) {
        val deadline = System.currentTimeMillis() + SERVER_START_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            if (log.isFile && log.readText().contains("Done (")) return
            if (!process.isAlive) {
                throw GradleException(
                    "The server exited with code ${process.exitValue()} before it finished starting." +
                        logTail(log)
                )
            }
            Thread.sleep(POLL_INTERVAL_MS)
        }
        process.destroyForcibly()
        throw GradleException("The server did not reach `Done (` within ${SERVER_START_TIMEOUT_MS / 1000} s." + logTail(log))
    }

    /** Runs the bot against the running server and returns its output. */
    private fun runBot(): String {
        val log = botLog.get().asFile
        log.parentFile.mkdirs()
        log.delete()

        // `node bot.mjs`, from the bot's own directory: that is what lets it resolve its `node_modules`.
        val process = try {
            ProcessBuilder("node", "bot.mjs")
                .directory(botDirectory.get().asFile)
                .redirectErrorStream(true)
                .redirectOutput(log)
                .start()
        } catch (error: IOException) {
            throw GradleException("The client test needs Node.js on this machine (`node --version`): ${error.message}", error)
        }
        val finished = process.waitFor(BOT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        if (!finished) process.destroyForcibly()
        val output = log.readText()

        // The bot's own account of what it saw, on the build's console: it is the half of this scenario
        // that no log assertion can show.
        logger.lifecycle("The client bot said:\n$output")
        if (!finished) {
            throw GradleException("The client bot did not finish within ${BOT_TIMEOUT_MS / 1000} s." + logTail(serverLog.get().asFile))
        }
        if (process.exitValue() != 0) {
            throw GradleException(
                "The client bot failed with exit code ${process.exitValue()}; its first failure is in the output above." +
                    logTail(serverLog.get().asFile)
            )
        }
        return output
    }

    /**
     * Installs the bot's dependencies with npm when they are missing or older than `package.json`.
     *
     * npm's cache is pointed into the build directory rather than left at its default: the pinned version
     * in `package.json` (and the lock file next to it) is then the only thing that decides what the bot
     * runs, whatever cache a machine or a CI runner happens to have. A CI runner installs here the same
     * way a workstation does; the task never assumes `node_modules` was checked in, and `.gitignore` keeps
     * it out of the repository.
     */
    private fun installBotDependencies() {
        val bot = botDirectory.get().asFile
        val nodeModules = bot.resolve("node_modules")
        val packageJson = File(bot, "package.json")
        if (nodeModules.isDirectory && nodeModules.lastModified() >= packageJson.lastModified()) return

        logger.lifecycle("Installing the client bot's dependencies (`npm install` in ${bot.name})...")
        val cache = npmCache.get().asFile
        cache.mkdirs()
        // npm on Windows is a `.cmd`, which CreateProcess cannot start on its own.
        val command = if (isWindows()) listOf("cmd.exe", "/c", "npm") else listOf("npm")
        val process = ProcessBuilder(command + listOf("install", "--no-audit", "--no-fund"))
            .directory(bot)
            .redirectErrorStream(true)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .apply { environment()["npm_config_cache"] = cache.absolutePath }
            .start()
        val exit = process.waitFor()
        if (exit != 0) {
            throw GradleException(
                "`npm install` failed with exit code $exit in ${bot.absolutePath}. The client test needs " +
                    "Node.js: check that `node --version` works on this machine."
            )
        }
    }

    /** Reads the server log and fails with every problem it found, or reports what the run covered. */
    private fun checkServerLog(log: File, botOutput: String) {
        val lines = log.readLines()
        val problems = mutableListOf<String>()

        if (lines.none { "XiaojieGUI has been enabled!" in it }) {
            problems += "The plugin never announced that it enabled."
        }
        // A name nothing logged is a missing check; a name that logged the wrong message is a check that
        // ran and found something else. Both are reported, with the message that was there instead.
        expectedDetails.get().forEach { (detail, expected) ->
            val marker = "XIAOJIE_SELFTEST detail: $detail ->"
            val line = lines.firstOrNull { marker in it }
            if (line == null) {
                problems += "The scenario never reported `$detail`.\n      absent: $marker"
                return@forEach
            }
            val message = line.substringAfter(marker)
            if (expected.isNotEmpty() && expected !in message) {
                problems += "The scenario reported `$detail`, but with the wrong message." +
                    "\n      present:  ${message.trim()}" +
                    "\n      expected: a message containing `$expected`"
            }
        }
        // The same prose Skript reports a broken element or an unparseable line in does not stop the
        // server, so the log is the only place it shows up -- and a client scenario that clicked a menu
        // nothing had parsed would otherwise look like a pass.
        fun forbid(description: String, pattern: Regex) {
            lines.firstOrNull { pattern.containsMatchIn(it) }?.let { problems += "$description\n      found: $it" }
        }
        forbid("Skript reported a severe error.", Regex("""\[Skript]\s+Severe Error"""))
        forbid("Skript could not compile a registered pattern.", Regex("pattern compiling exception"))
        forbid("A script line could not be understood.", Regex("""Can't understand this"""))
        forbid(
            "An expression was used outside the event it belongs to.",
            Regex("""may only be used in an? [a-z ]*event""")
        )
        forbid("The scenario reported a failed check.", Regex("""XIAOJIE_SELFTEST detail: .*FAILED"""))

        val undeclared = undeclaredDetails.get()
        if (undeclared.isNotEmpty()) {
            problems += "The scenario reports names the test does not expect: ${undeclared.joinToString(", ")}"
        }

        if (problems.isNotEmpty()) {
            throw GradleException(
                buildString {
                    append("The client test found ${problems.size} problem(s) in ${log.absolutePath}:")
                    problems.forEach { append("\n  - ").append(it) }
                    append("\n  - the bot said:\n").append(botOutput.trim().ifEmpty { "(nothing)" })
                }
            )
        }
        logger.lifecycle(
            "Client test passed: the bot saw the menu, made its clicks, and all " +
                "${expectedDetails.get().size} scenario line(s) are in the server log."
        )
    }

    /** Stops the server the way a console would, and kills it only if that does not work. */
    private fun stopServer(process: Process) {
        if (!process.isAlive) return
        runCatching {
            // Paper reads console commands from stdin, so `stop` is the same shutdown an operator asks
            // for: the world is saved and the plugins are disabled. Killing the process cannot do that.
            process.outputStream.write("stop\n".toByteArray())
            process.outputStream.flush()
        }.onFailure { logger.warn("Could not write `stop` to the server: ${it.message}") }

        if (!process.waitFor(SERVER_STOP_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            logger.warn("The server did not stop within ${SERVER_STOP_TIMEOUT_MS / 1000} s of `stop`; killing it.")
            process.destroyForcibly()
            process.waitFor(SERVER_STOP_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        }
    }

    /** The end of a log, for a failure message that has to say what the run actually did. */
    private fun logTail(log: File): String {
        if (!log.isFile) return "\n  (no log at ${log.absolutePath})"
        return "\n  --- the last $LOG_TAIL_LINES lines of ${log.absolutePath} ---\n" +
            log.readLines().takeLast(LOG_TAIL_LINES).joinToString("\n") { "  $it" }
    }

    private fun isWindows(): Boolean = System.getProperty("os.name").lowercase().contains("win")

    companion object {
        private const val SERVER_START_TIMEOUT_MS = 5 * 60 * 1000L
        private const val SERVER_STOP_TIMEOUT_MS = 60 * 1000L
        private const val BOT_TIMEOUT_MS = 120 * 1000L
        private const val POLL_INTERVAL_MS = 500L
        private const val LOG_TAIL_LINES = 40
    }
}
