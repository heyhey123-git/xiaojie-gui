import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import xyz.jpenilla.runpaper.task.RunServer
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
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
// The scripts of the client scenario, whose `XIAOJIE_SELFTEST` lines need a player and are therefore
// declared next to the bot's run (`clientTestExpectedDetails`) rather than next to the server test's.
// Both undeclared-name checks read every script, so both have to know which files to leave out.
val clientOnlyScripts = setOf("11-client.sk", "12-client-drag.sk")

val serverTestUndeclaredDetails = providers.provider {
    val reported = serverTestScripts.asFile.walkTopDown()
        .filter { it.isFile && it.extension == "sk" && it.name !in clientOnlyScripts }
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
//
// What the Via layer in this run proves, and what it does not
// ---------------------------------------------------------
// The bot is a **26.1** client (`minecraft-data` ships no 26.2 data) and says so in its handshake; the
// server is the same Paper **26.2** the other layer boots. ViaVersion + ViaBackwards sit between them
// and translate 26.2 down to 26.1 on the way out, which is why this is a real test of the addon's own
// packets rather than of a lie:
//
//   * the client's handshake states 26.1 (protocol 775) and Via answers it as such, so nothing is
//     pretended at login and both play packets 26.2 reshaped (login success, join game) are read in the
//     26.1 shape the client can actually parse;
//   * every window, click and title this scenario asserts still went through the addon first -- Via only
//     rewrites what it is handed, so a packet the addon never sent cannot be seen here;
//   * and because ViaBackwards rewrites the *item registry* 26.2 -> 26.1, the item ids that arrive are
//     26.1 ids and the client can name the item in a slot. That is what makes `11-client.sk`'s icons
//     assertable as types at all, and it is the one thing the handshake lie could never do.
//
// What it does **not** prove is that a real 26.2 client sees these packets: what the client parses is
// Via's translation of the addon's output, not the addon's output itself. Two consequences worth saying
// out loud, because they are the boundary of this layer:
//
//   * a 26.2-only shape the addon sends and Via rewrites correctly would still be seen as a 26.1 shape
//     here, so this layer cannot see the difference between "the addon sent the right 26.2 bytes" and
//     "the addon sent bytes Via could translate";
//   * a 26.2-only shape the addon sends wrongly enough that Via cannot translate it is a translation
//     error in this log, not necessarily a client error -- and, in the other direction, Via's own
//     translation bugs would look like the addon's here.
//
// A real 26.2 client would settle both. Until the suite has one, this is the closest a run can get: the
// addon's packets survive a real translation layer, which is what a real client on that version would
// find on the other side of one.

val clientTestWork = layout.buildDirectory.dir("client-test")
val clientBotDirectory = layout.projectDirectory.dir("server-test/client")

// The two plugins that let the client test's bot be an honest 26.1 client of a 26.2 server.
//
// `serverTest` deliberately does not get these: it never has a client, so there is nothing to translate
// and the layer it tests is the plugin's own. `clientTest` copies them into `plugins/` for its run and
// takes them out again, the same way it does with the jar under test, so a `serverTest` that follows in
// the same build still boots a server with no translation layer in it.
//
// ViaBackwards is the direction this test needs and ViaVersion is not: ViaVersion lets a *newer* client
// join an older server, ViaBackwards lets an *older* client join a newer one, and ViaBackwards declares
// ViaVersion as a required dependency. Both are pinned to 5.12.0 because that is one release of the pair
// whose changelogs name 26.2/26.3, and ViaBackwards ships the 26.2-to-26.1 protocol and its item-id
// mapping (`assets/viabackwards/data/mappings-26.2to26.1.nbt`) -- the mapping that makes an item type
// assertable on the client at all. The versions have to match: a ViaVersion that does not know 26.2
// would leave ViaBackwards unable to load its protocol.
//
// The Modrinth CDN URL is used for the same reason as SkBee's in `serverTestPlugins` above: it names the
// exact file of the exact release. Downloaded by this build into `clientTestPluginDirectory` rather than
// by the server on first boot, so the assets stay visible next to that reason.
val clientTestPlugins = mapOf(
    "ViaVersion-5.12.0.jar" to
        "https://cdn.modrinth.com/data/P1OZGk5p/versions/FaishMnD/ViaVersion-5.12.0.jar",
    "ViaBackwards-5.12.0.jar" to
        "https://cdn.modrinth.com/data/NpvuJQoq/versions/SxGhdsPK/ViaBackwards-5.12.0.jar"
)

// Where those two jars are cached. Under `build/`, like the rest of what a run downloads, so the cache
// is disposable and a machine that has run this task once does not download six megabytes again.
val clientTestPluginDirectory = layout.buildDirectory.dir("client-test-plugins")

// One entry per `XIAOJIE_SELFTEST detail:` name `11-client.sk` reports, mapped to a substring its message
// has to contain. The same shape as `serverTestExpectedDetails` above and for the same reason: a line
// that exists but reports another slot or another click type fails here, so the values are the ones the
// run this file was written against reported.
//
// The item assertions are the other half and they live in the bot, not here: which item type and which
// `custom_name` each slot holds is something only the client can read, so `server-test/client/bot.mjs`
// checks it and its own failure is what fails this task. The bot's output is printed to the console when
// it runs, which is where those checks are visible.
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
    "client page turned" to "1 -> 2",
    // The drag scenario (`12-client-drag.sk`), which needs a `static` menu because a drag is the one way
    // a real inventory can be changed without a click: the menu's own layout, a drag that the game
    // rewrote into a click because the cursor held one item, a drag that filled two slots and was
    // reported as one interaction, and a drag a script refused -- which has to refuse all of it.
    "client drag menu opened" to "A at 0",
    "client one slot drag came in as a click" to "slot 11, type left mouse button",
    "client multi slot drag" to "slots 13 and 14, type left mouse button",
    "client drag onto protected slots" to "cancelled for slots 5 and 6, cursor 2",
    // `locked icons`: the script writes no cancellation at all, and the bot reads its own window back to
    // check that the icon is still there after a click and a shift click. That last line reads a window
    // with the two expressions a menu which keeps things in its free slots is saved with.
    "locked menu read" to "slots 0 and 1, 2 item(s)"
)

// The names the client scenario's scripts report that the map above does not declare.
// `serverTestUndeclaredDetails` leaves those files out (their lines need a player), so this is where
// those names are held to the same rule: a line in the client scenario that nothing declares fails the
// build. Every script the bot's run drives has to be listed here, or its lines go unchecked.
val clientTestUndeclaredDetails = providers.provider {
    val reported = clientOnlyScripts
        .flatMap { serverTestScripts.file(it).asFile.readLines() }
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
    viaPlugins.set(clientTestPlugins)
    viaPluginCache.set(clientTestPluginDirectory)
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

    /** The translation layer's plugins, as file name to download URL; see `clientTestPlugins`. */
    @get:Input
    abstract val viaPlugins: MapProperty<String, String>

    /** Where [viaPlugins] are cached between runs. */
    @get:Internal
    abstract val viaPluginCache: DirectoryProperty

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
        // The translation layer is cached under `build/` and copied into the run directory's `plugins/`,
        // which does not have it: `serverTest` has to keep booting a server that translates nothing.
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
        val copiedPluginJar: File,
        /** The translation layer jars this task copied into `plugins/`, so they can be taken out again. */
        val copiedViaJars: List<File>
    )

    /**
     * Makes the run directory this task's own copy of the prepared one, and reports what it changed.
     *
     * run-paper hands the shaded jar to the server as a plugin path of its own; this task starts the
     * server the way Paperclip re-launches it, from the run directory, where Paper finds plugins in
     * `plugins/`. A jar left there by an earlier run would make Paper report one plugin name for two
     * files and skip the plugin, so the directory is left holding exactly the jar under test, plus the
     * two translation-layer jars this scenario needs.
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
        // The translation layer is copied in here, next to the jar under test, because it is part of the
        // same "this run's plugins" set and has to come out again for the same reason.
        return RunDirectoryChanges(hidden, copied, copyViaPluginsInto(dir))
    }

    /**
     * Downloads the translation layer's jars into the cache under `build/` if they are not there, and
     * copies them into the run directory's `plugins/`, returning what it copied.
     *
     * The cache is keyed by file name, which carries the version, so changing the pin in
     * `clientTestPlugins` downloads the new release instead of reusing the old bytes.
     */
    private fun copyViaPluginsInto(dir: File): List<File> {
        val cache = viaPluginCache.get().asFile
        cache.mkdirs()
        val plugins = dir.resolve("plugins")
        return viaPlugins.get().map { (name, url) ->
            val cached = cache.resolve(name)
            if (!cached.isFile) {
                logger.lifecycle("Downloading $name")
                URI(url).toURL().openStream().use { input -> cached.outputStream().use { input.copyTo(it) } }
            }
            cached.copyTo(plugins.resolve(name), overwrite = true)
        }
    }

    /**
     * Puts the run directory back the way `prepareServerTest` leaves it.
     *
     * The copied plugin jars go, because run-paper supplies the same jar itself and two files claiming
     * one plugin name is the `Ambiguous plugin name` error the next `serverTest` would otherwise boot
     * with. That includes the translation layer: leaving it behind would silently change what the other
     * layer tests. The generated examples script comes back, because Gradle is free to run this task
     * before `serverTest` in the same build and that run is where the examples are parsed.
     */
    private fun cleanUpRunDirectory(changes: RunDirectoryChanges) {
        changes.copiedPluginJar.delete()
        changes.copiedViaJars.forEach { it.delete() }
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

// --- SkriptHub documentation ------------------------------------------------------------------------
//
// SkriptHub does not read a jar: it reads a JSON file that SkriptHubDocsTool writes *on a running
// server* that has the addon loaded, because half of what an element says about itself (its patterns,
// its return type, the event values that belong to its event) only exists once Skript has registered
// it. `./gradlew gendocs` does the whole thing locally -- it boots the same disposable Paper server
// `serverTest` uses, has it run the tool's `/gendocs` console command, and leaves the JSON under
// `build/`, which is what a person pastes into the SkriptHub dashboard's JSON import.
//
// `/gendocs` writes its file locally and needs no account, no token and no network: the upload is the
// dashboard import, done by hand. That is the reason this task can be a build step at all.
//
// What the tool can and cannot see is the question this task exists to answer, and the answer is in
// the generated file rather than in the tool's own logs: it enumerates Skript's *legacy* registry
// accessors (`Skript.getEffects()` and friends), and Skript 2.16 answers those out of the same
// `SyntaxRegistry` the addon API writes to, so syntax registered the way `SkriptSyntax.kt` registers it
// does reach the file. `GendocsReport` below reports what it found so that a re-run says the same
// thing without anyone opening the JSON by hand.

// The tool is a server plugin published as a GitHub release asset and has no Maven coordinates, so the
// URL is the pin, exactly as it is for `serverTestPlugins`. 1.17 is the release that knows the
// `org.skriptlang.skript` API this addon registers through.
val skriptHubDocsToolVersion = "1.17"
val skriptHubDocsToolUrl =
    "https://github.com/SkriptHub/SkriptHubDocsTool/releases/download/" +
        "$skriptHubDocsToolVersion/skripthubdocstool-$skriptHubDocsToolVersion.jar"
val skriptHubDocsToolJar = layout.buildDirectory.file("tools/skripthubdocstool-$skriptHubDocsToolVersion.jar")

// The run directory the documentation server gets. It is not `server-test/run`:
//
//   * `server-test/skript/*.sk` stops the server ten seconds after it is up (`99-finish.sk`), which is
//     the opposite of what this run wants -- it has to be up long enough to be told `/gendocs`;
//   * the tool's jar cannot be left where `serverTest` boots from, and a run directory of its own is a
//     stronger guarantee of that than a cleanup step anything can skip.
//
// Everything below `build/` is a build product, so this directory is disposable: `clean` removes it.
val skriptHubDocsDirectory = layout.buildDirectory.dir("gendocs-server")
val skriptHubDocsJson = layout.buildDirectory.file("skripthub/xiaojie-gui.json")

// The server this task boots is the one `runServer` downloaded, copied rather than downloaded again: a
// second Paper of the same version under `build/` would be eighty megabytes of duplication for no
// difference. Only what a boot needs is copied -- the patched jar, the libraries it extracted next to
// it, Mojang's client jar from `cache/` and Paper's own `config/` -- and the world and logs are left
// behind so each run starts from a clean one.
//
// This list lives inside the task that uses it rather than beside it: a nested class in this script may
// not read a script-level `val`, because the Kotlin DSL compiler answers such a read through a
// *synthetic accessor on the script object* and then has to compile the class as an **inner** class --
// which Gradle refuses to instantiate as a task ("is a non-static inner class"). Only classes and
// members the task itself declares are safe.

val downloadSkriptHubDocsTool by tasks.registering {
    description = "Downloads the tool that generates a SkriptHub documentation JSON."
    group = "documentation"

    val tool = skriptHubDocsToolJar
    inputs.property("url", skriptHubDocsToolUrl)
    outputs.file(tool)

    doLast {
        val jar = tool.get().asFile
        // The version is in the file name, so a file that is already here is the file the URL names.
        if (jar.isFile && jar.length() > 0L) {
            logger.lifecycle("${jar.name} is already downloaded.")
            return@doLast
        }
        jar.parentFile.mkdirs()
        logger.lifecycle("Downloading ${jar.name}")
        // Written beside the real name and moved onto it, so an interrupted transfer is never taken
        // for the complete file by the next run. Replacing is what makes a second download work on
        // Windows, where a plain rename refuses to overwrite.
        val partial = File(jar.parentFile, jar.name + ".part")
        URI(skriptHubDocsToolUrl).toURL().openStream().use { input ->
            partial.outputStream().use { input.copyTo(it) }
        }
        Files.move(
            partial.toPath(),
            jar.toPath(),
            StandardCopyOption.REPLACE_EXISTING
        )
    }
}

/**
 * Boots the documentation server, runs `/gendocs` on its console, and leaves the JSON the tool wrote.
 *
 * This is `ClientTest`'s process handling, not run-paper's: run-paper runs a server in the foreground
 * and stops it when the *server* says so, and this task has to decide when, because it is waiting for a
 * file the server produces. So the server is a background process of this task, its console is stdin,
 * and `stop` goes there like an operator's would.
 */
abstract class GenerateGendocs : DefaultTask() {

    /** The run directory `serverTest` prepared; only its server and libraries are used. */
    @get:Internal
    abstract val sourceServerDirectory: DirectoryProperty

    /** The documentation server's own run directory, under `build/`. */
    @get:Internal
    abstract val runDirectory: DirectoryProperty

    /** The Minecraft version the run directory (and so the server jar) is for. */
    @get:Input
    abstract val minecraftVersion: Property<String>

    /** The shaded jar, which is the addon the documentation is generated for. */
    @get:Internal
    abstract val pluginJar: RegularFileProperty

    /** The plugins the server needs besides ours, as file name to download URL. */
    @get:Input
    abstract val serverPlugins: MapProperty<String, String>

    /** The SkriptHubDocsTool jar, downloaded by `downloadSkriptHubDocsTool`. */
    @get:Internal
    abstract val docsToolJar: RegularFileProperty

    /** Where the tool writes its documentation, relative to the run directory. */
    @get:Internal
    abstract val generatedFile: RegularFileProperty

    /** The Java the server runs on, which Paper 26.2 requires to be 25. */
    @get:Input
    abstract val javaExecutable: Property<String>

    /** Where this task writes the server's console output for the run. */
    @get:Internal
    abstract val serverLog: RegularFileProperty

    @TaskAction
    fun generate() {
        val source = sourceServerDirectory.get().asFile
        val paperJar = source.resolve("versions/${minecraftVersion.get()}/paper-${minecraftVersion.get()}.jar")
        if (!paperJar.isFile) {
            throw GradleException(
                "The prepared server is missing $paperJar, and this task copies the server `serverTest` " +
                    "downloaded rather than downloading a second one. Run `./gradlew serverTest` once, " +
                    "then this task again."
            )
        }

        val dir = runDirectory.get().asFile
        val log = serverLog.get().asFile
        prepare(dir, source, paperJar, log)
        val process = start(dir, paperJar, log)
        try {
            awaitServerReady(process, log)
            // `/gendocs` from the console, one line, exactly as an operator would type it. The tool
            // answers on stdout, which is the log this task reads back afterwards.
            process.outputStream.write("/gendocs\n".toByteArray())
            process.outputStream.flush()
            awaitGeneratedFile(process, log)
        } finally {
            stopServer(process)
        }
    }

    /**
     * Makes [dir] a server that can boot and that has exactly the plugins this run needs in it.
     *
     * The directory is rebuilt from scratch every time rather than kept: Paper, Skript and the tool all
     * write state into it (Skript rewrites its config, the tool writes its output), and a stale copy of
     * any of that is a bug that only shows up on the second run.
     */
    private fun prepare(dir: File, source: File, paperJar: File, log: File) {
        dir.deleteRecursively()
        dir.mkdirs()
        // The server and its libraries come from the prepared run directory, which is where `runServer`
        // put them. `libraries/` matters: Paper 26.2 does not boot from `java -jar`, which is why the
        // classpath below is the paper jar followed by every jar in there.
        BOOTSTRAPPED_DIRECTORIES.forEach { name ->
            val from = source.resolve(name)
            if (from.isDirectory) from.copyRecursively(dir.resolve(name), overwrite = true)
        } // Paper refuses to start without this. As in the server test, it records acceptance of the
        // Minecraft EULA (https://aka.ms/MinecraftEULA) for this disposable server and no other.
        dir.resolve("eula.txt").writeText("eula=true\n")
        dir.resolve("server.properties").writeText(gendocsServerProperties())

        val plugins = dir.resolve("plugins")
        plugins.mkdirs()
        // A flat world and no players, so the boot is as short as it can be and the console has no
        // player events to interleave with the tool's output.
        serverPlugins.get().forEach { (name, url) ->
            val jar = plugins.resolve(name)
            if (!jar.isFile) {
                logger.lifecycle("Downloading $name")
                URI(url).toURL().openStream().use { input -> jar.outputStream().use { input.copyTo(it) } }
            }
        }
        docsToolJar.get().asFile.copyTo(plugins.resolve(docsToolJar.get().asFile.name), overwrite = true)
        // The addon under test goes in last, so the file the documentation describes is this build's.
        pluginJar.get().asFile.copyTo(plugins.resolve(pluginJar.get().asFile.name), overwrite = true)
        // No scripts: `server-test/skript/*.sk` reports self-test lines and one of them stops the
        // server, and this run wants neither. An empty scripts folder is what Skript reads.
        dir.resolve("plugins/Skript/scripts").mkdirs()
        log.parentFile.mkdirs()
        log.delete()
    }

    /**
     * The properties the documentation server runs with.
     *
     * The port is the one `server-test/server.properties` uses, because this task never runs at the same
     * time as the layers that boot that server. The pause is off for the reason `runServer` turns it off:
     * an empty server would stop ticking, and Skript's scheduler -- which is what the tool's own
     * generation runs on -- would stop with it.
     */
    private fun gendocsServerProperties(): String = """
        online-mode=false
        server-port=25598
        level-type=minecraft:flat
        generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}
        level-name=world
        spawn-protection=0
        max-players=1
        view-distance=2
        simulation-distance=2
        difficulty=peaceful
        enable-command-block=false
        enable-status=false
        pause-when-empty-seconds=-1
        sync-chunk-writes=false
    """.trimIndent() + "\n"

    /**
     * Starts the server the way Paperclip re-launches the patched jar: from the command line, because
     * `java -jar` on it ends in `NoClassDefFoundError`.
     *
     * The order matters. `libraries/` holds an older `com.mojang:logging` than the patched jar carries,
     * and with the libraries first the server dies in `LogUtils.getClassLogger()` before it opens a port.
     */
    private fun start(dir: File, paperJar: File, log: File): Process {
        val classpath = buildList {
            add(paperJar.absolutePath)
            dir.resolve("libraries").walkTopDown()
                .filter { it.isFile && it.extension == "jar" }
                .forEach { add(it.absolutePath) }
        }.joinToString(File.pathSeparator)

        return ProcessBuilder(
            javaExecutable.get(),
            "-Xms1G",
            "-Xmx1G",
            "-cp",
            classpath,
            "org.bukkit.craftbukkit.Main",
            "--nogui"
        )
            // The run directory is what makes the server find its own `server.properties` and plugins.
            .directory(dir)
            .redirectErrorStream(true)
            .redirectOutput(log)
            .start()
    }

    /** Waits for the server to finish starting, or fails with what the log holds instead. */
    private fun awaitServerReady(process: Process, log: File) {
        val deadline = System.currentTimeMillis() + SERVER_START_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            if (log.isFile && log.readText().contains("Done (")) return
            if (!process.isAlive) {
                throw GradleException(
                    "The documentation server exited with code ${process.exitValue()} before it finished " +
                        "starting." + logTail(log)
                )
            }
            Thread.sleep(POLL_INTERVAL_MS)
        }
        throw GradleException(
            "The documentation server did not reach `Done (` within ${SERVER_START_TIMEOUT_MS / 1000} s." +
                logTail(log)
        )
    }

    /**
     * Waits for the file `/gendocs` writes, and reports the server's own console output as the failure
     * if it never appears.
     *
     * A file that exists is not a file that is complete: the tool opens it and then writes, so a size
     * that has stopped changing is the signal to read it, not its existence.
     */
    private fun awaitGeneratedFile(process: Process, log: File) {
        val file = generatedFile.get().asFile
        val deadline = System.currentTimeMillis() + GENDOCS_TIMEOUT_MS
        var settledSize = -1L
        while (System.currentTimeMillis() < deadline) {
            if (file.isFile) {
                val size = file.length()
                if (size > 0L && size == settledSize) return
                settledSize = size
            }
            if (!process.isAlive) {
                throw GradleException(
                    "The documentation server exited with code ${process.exitValue()} before the tool " +
                        "wrote ${file.absolutePath}." + logTail(log)
                )
            }
            Thread.sleep(POLL_INTERVAL_MS)
        }
        throw GradleException(
            "The documentation tool wrote no ${file.absolutePath} within ${GENDOCS_TIMEOUT_MS / 1000} s of " +
                "`/gendocs`. Its own console output is in the log." + logTail(log)
        )
    }

    /** Stops the server the way a console would, and kills it only if that does not work. */
    private fun stopServer(process: Process) {
        if (!process.isAlive) return
        runCatching {
            // Paper reads console commands from stdin, so `stop` is the same shutdown an operator asks
            // for: the world is saved and the plugins are disabled. Killing the process cannot do that,
            // and the tool has already written its file by the time this is called.
            process.outputStream.write("stop\n".toByteArray())
            process.outputStream.flush()
        }.onFailure { logger.warn("Could not write `stop` to the documentation server: ${it.message}") }

        if (!process.waitFor(SERVER_STOP_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            logger.warn("The documentation server did not stop within ${SERVER_STOP_TIMEOUT_MS / 1000} s of `stop`; killing it.")
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

    companion object {
        /** What `prepare` copies out of the prepared run directory; see the note above the task. */
        private val BOOTSTRAPPED_DIRECTORIES = listOf("versions", "libraries", "cache", "config")

        private const val SERVER_START_TIMEOUT_MS = 5 * 60 * 1000L
        private const val SERVER_STOP_TIMEOUT_MS = 60 * 1000L
        private const val GENDOCS_TIMEOUT_MS = 180 * 1000L
        private const val POLL_INTERVAL_MS = 500L
        private const val LOG_TAIL_LINES = 40
    }
}

/**
 * Reads the file `/gendocs` wrote, reports what is in it, and leaves it under `build/`.
 *
 * The reporting is the point of the task: SkriptHub cannot be asked what it sees, so the only way to
 * know whether a publishing step has anything to import is to say out loud what the generated file
 * holds. This counts the entries per kind and names every one, which is what makes "the addon's syntax
 * is in there" a check rather than a hope -- and what makes it visible when a Skript upgrade stops the
 * tool from seeing the addon at all.
 *
 * The file itself is copied out of the run directory because the run directory is rebuilt from scratch
 * each time: `build/skripthub/xiaojie-gui.json` is the artifact a person uploads.
 */
abstract class GendocsReport : DefaultTask() {

    /** What the tool wrote on the server that just ran. */
    @get:InputFile
    abstract val generated: RegularFileProperty

    /** Where the generated file is left, for a person to paste into SkriptHub. */
    @get:OutputFile
    abstract val destination: RegularFileProperty

    @TaskAction
    fun collect() {
        val source = generated.get().asFile
        if (!source.isFile) {
            throw GradleException(
                "The documentation tool wrote no ${source.absolutePath}, so either the server never ran " +
                    "it or it failed to. `generateSkriptHubDocs` would have failed first in either case; " +
                    "its console output is in build/gendocs-server.log."
            )
        }

        @Suppress("UNCHECKED_CAST")
        val document = groovy.json.JsonSlurper().parse(source) as Map<String, Any?>
        // Every kind the tool can write. An addon that uses none of them is not an error, so the kinds
        // this addon has no elements of are not required to be there -- but every one that is there is
        // reported, which is how a kind going missing gets noticed.
        val kinds = listOf(
            "events",
            "conditions",
            "effects",
            "expressions",
            "types",
            "functions",
            "sections",
            "structures"
        )
        val entries = kinds.map { kind ->
            kind to (document[kind] as? List<*> ?: emptyList<Any?>()).filterIsInstance<Map<*, *>>()
        }
        val total = entries.sumOf { it.second.size }
        val metadata = document["metadata"] as? Map<*, *>

        val report = buildString {
            appendLine("The SkriptHub documentation tool wrote ${source.name}:")
            appendLine("  metadata: version='${metadata?.get("version")}' apiVersion='${metadata?.get("apiVersion")}'")
            appendLine("  $total element(s) in total")
            entries.filter { it.second.isNotEmpty() }.forEach { (kind, ofKind) ->
                appendLine("  $kind (${ofKind.size}):")
                ofKind.forEach { entry ->
                    // The tool writes an element's name and its patterns; the patterns are what a reader
                    // recognises a syntax by, so both are printed.
                    val patterns = (entry["patterns"] as? List<*>).orEmpty().joinToString(" | ") { it.toString() }
                    appendLine("    - ${entry["name"]} :: $patterns")
                }
            }
        }
        // On the console rather than in the file: the file is an artifact, and this is the answer to
        // "did the addon's syntax get documented or not".
        logger.lifecycle(report)

        val target = destination.get().asFile
        target.parentFile.mkdirs()
        source.copyTo(target, overwrite = true)
        logger.lifecycle(
            "The generated documentation is at ${target.absolutePath} " +
                "($total element(s)), ready for SkriptHub's JSON import."
        )
    }
}

val generateSkriptHubDocs by tasks.registering(GenerateGendocs::class) {
    description = "Boots a server carrying this addon and SkriptHubDocsTool and runs `/gendocs` on it."
    group = "documentation"
    dependsOn(downloadSkriptHubDocsTool, tasks.named("prepareServerTest"), tasks.named("shadowJar"))

    sourceServerDirectory.set(serverTestDirectory)
    runDirectory.set(skriptHubDocsDirectory)
    minecraftVersion.set(paperMinecraftVersion)
    pluginJar.set(tasks.named<ShadowJar>("shadowJar").flatMap { it.archiveFile })
    serverPlugins.set(serverTestPlugins)
    docsToolJar.set(skriptHubDocsToolJar)
    // Named after the plugin, which is the name SkriptHub knows the addon by: the tool writes one file
    // per addon and names each after that addon's plugin.
    generatedFile.set(
        skriptHubDocsDirectory.map { it.file("plugins/SkriptHubDocsTool/documentation/xiaojie-gui.json") }
    )
    javaExecutable.set(
        javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(25) }
            .map { it.executablePath.asFile.absolutePath }
    )
    serverLog.set(layout.buildDirectory.file("gendocs-server.log"))
    // A task that boots a server is never up to date: reporting success because nothing it reads has
    // changed would mean reporting success without having generated anything.
    outputs.upToDateWhen { false }
}

val gendocs by tasks.registering(GendocsReport::class) {
    description = "Generates the SkriptHub documentation JSON from a server carrying this addon."
    group = "documentation"
    dependsOn(generateSkriptHubDocs)
    generated.set(
        skriptHubDocsDirectory.map { it.file("plugins/SkriptHubDocsTool/documentation/xiaojie-gui.json") }
    )
    destination.set(skriptHubDocsJson)
}
