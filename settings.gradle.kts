pluginManagement {
    repositories {
        gradlePluginPortal()
        // A mirror of Maven Central, for networks where Central refuses a request: Gradle treats a 403 as
        // a hard failure for that repository instead of trying the next one.
        //
        // The property is read here rather than from a variable above, because `pluginManagement { }` is
        // compiled as its own script and cannot see the settings script's locals. Enable it with
        // `-PcnMirror`, or by adding `cnMirror=true` to ~/.gradle/gradle.properties.
        if (providers.gradleProperty("cnMirror").isPresent) {
            maven("https://maven.aliyun.com/repository/public")
        }
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "xiaojie-gui"
