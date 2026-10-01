rootProject.name = "detekt-rules"

pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":core")
include(":rules:architecture")
include(":rules:compose")
include(":rules:coroutines")
include(":rules:gradle")
include(":rules:style")
include(":rules:testing")
