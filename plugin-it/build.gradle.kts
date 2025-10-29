import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.gradleup.shadow") version "9.0.0-beta15"
    id("org.jetbrains.kotlin.jvm") version "2.2.21"
}

group = "io.github.heyhey123.xiaojiegui.it"
version = "1.0-SNAPSHOT"
val kotlinVersion = "2.2.21"
val shadePrefix: String by project

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly(project(":"))
    compileOnly(kotlin("stdlib"))

    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")

//    implementation(platform("org.junit:junit-bom:5.10.0"))
//    implementation("org.junit.jupiter:junit-jupiter")
//    runtimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(21)
}

tasks {
    base {
        archivesName.set("pluginit")
    }
    withType<ShadowJar> {
        archiveAppendix.set("")
        archiveClassifier.set("")
        archiveVersion.set(version as String)
        destinationDirectory.set(file("$projectDir/build/dist"))

        minimize()

        val kotlinEscapedVersion = kotlinVersion.filter { it != '.' }

//        relocate("org.junit", "io.github.heyhey123.it.shaded.junit")
//        relocate("org.opentest4j", "io.github.heyhey123.it.shaded.opentest4j")
//        relocate("org.apiguardian", "io.github.heyhey123.it.shaded.apiguardian")

        relocate("kotlin.", "$shadePrefix.kotlin${kotlinEscapedVersion}.")
        relocate("org.jetbrains.annotations.", "$shadePrefix.org.jetbrains.annotations2602.")
    }

    build {
        dependsOn("shadowJar")
    }
}
