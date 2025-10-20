import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    java
    id("com.gradleup.shadow") version "9.0.0-beta15"
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18"
}

group = "io.github.heyhey123"
version = "1.0-SNAPSHOT"
val kotlinVersion = "2.0.0"

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
    withType<ShadowJar> {
        val kotlinEscapedVersion = kotlinVersion.filter { it != '.' }
        archiveAppendix.set("")
        archiveClassifier.set("")
        archiveVersion.set(version as String)
        destinationDirectory.set(file("$rootDir/build/dist"))

        exclude("io.github.heyhey123.xiaojiegui/testing/**") // please comment out in production
        minimize()

        //relocate Kotlin
        relocate("kotlin.", "${rootProject.group}.kotlin${kotlinEscapedVersion}.")
        relocate("org.jetbrains.annotations.", "${rootProject.group}.org.jetbrains.annotations2602.")
        relocate("xyz.jpenilla.reflectionremapper", "${rootProject.group}.xyz.jpenilla.reflectionremapper013")
        relocate("net.fabricmc.mappingio.", "${rootProject.group}.net.fabricmc.mappingio071.")
    }
    build {
        dependsOn(shadowJar)
    }
    test {
        useJUnitPlatform()
    }
}
