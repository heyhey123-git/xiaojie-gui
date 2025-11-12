import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.dokka.gradle.tasks.DokkaGenerateTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.net.URI


plugins {
    java
    id("com.gradleup.shadow") version "9.0.0-beta15"
    id("org.jetbrains.kotlin.jvm") version "2.2.21"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18"
    id("org.jetbrains.dokka") version "2.1.0"
}

group = "io.github.heyhey123"
version = "1.0.1"
val kotlinVersion = "2.2.21"
val shadePrefix: String by project

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.skriptlang.org/releases")
    maven("https://repo.destroystokyo.com/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
}

dependencies {
    implementation(kotlin("stdlib"))
    paperweight.paperDevBundle("1.21.8-R0.1-SNAPSHOT")
    compileOnly("com.github.SkriptLang:Skript:2.12.1")
    compileOnly("com.github.retrooper:packetevents-spigot:2.9.5")
    implementation("xyz.jpenilla:reflection-remapper:0.1.3")

    testImplementation(kotlin("test"))
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.0.0")
    testImplementation("io.mockk:mockk:1.14.6")
}

paperweight {
    addServerDependencyTo = configurations.named(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME).map { setOf(it) }
    // exclude the paperweight provided artifact at test time
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
        freeCompilerArgs.addAll("-Xallow-unstable-dependencies")
    }
}

tasks {
    base {
        archivesName.set("xiaojiegui")
    }

    withType<ShadowJar> {
        val kotlinEscapedVersion = kotlinVersion.filter { it != '.' }
        archiveAppendix.set("")
        archiveClassifier.set("")
        archiveVersion.set(version as String)
        destinationDirectory.set(file("$rootDir/build/dist"))

        minimize()

        //relocate Kotlin
        relocate("kotlin.", "$shadePrefix.kotlin${kotlinEscapedVersion}.")
        relocate("org.jetbrains.annotations.", "$shadePrefix.org.jetbrains.annotations2602.")
        relocate("net.fabricmc.", "$shadePrefix.net.fabricmc.")
        relocate("xyz.jpenilla.reflectionremapper.", "$shadePrefix.xyz.jpenilla.reflectionremapper.")
    }

    build {
        dependsOn(shadowJar)
    }

    test {
        useJUnitPlatform()
    }

    register("rootBuild") {
        group = "build"
        description = "Build root project only"
        dependsOn(":build")
    }
    dokka {
        dokkaPublications.html {
            suppressInheritedMembers = true
        }

        dokkaSourceSets.main {
            sourceRoots.from(file("src"))
            documentedVisibilities.set(setOf(VisibilityModifier.Public))
            jdkVersion = 21
            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl.set(URI("https://github.com/heyhey123-git/xiaojie-gui/tree/master/src/main/kotlin"))
                remoteLineSuffix.set("#L")
            }

            externalDocumentationLinks {
                register("skript") {
                    url("https://docs.skriptlang.org/javadocs/")
                    packageListUrl("https://docs.skriptlang.org/javadocs/element-list")
                }
                register("paper") {
                    url("https://jd.papermc.io/paper/1.21.10/")
                    packageListUrl("https://jd.papermc.io/paper/1.21.10/element-list")
                }
            }
        }

        pluginsConfiguration.html {
//            customStyleSheets.from("styles.css")
//            customAssets.from("logo.png")
            footerMessage.set("Copyright (c) 2025 heyhey123, All rights reserved.")
        }

    }
    withType<DokkaGenerateTask>().configureEach {
        outputDirectory.set(layout.buildDirectory.dir("buildDocs/dokka"))
    }
}
