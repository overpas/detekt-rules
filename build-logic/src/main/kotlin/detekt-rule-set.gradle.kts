import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("code-coverage")
    id("com.gradleup.shadow")
    id("jvm-lib")
    id("static-analysis")
}

val libs = the<LibrariesForLibs>()
val corePackage = "by.overpas.detekt.core"

dependencies {
    implementation(project(":core"))

    compileOnly(libs.detekt.api)

    testImplementation(libs.detekt.test)
    testImplementation(libs.kotlin.test)
}

tasks.shadowJar {
    archiveBaseName = "detekt-rules-${project.name}"
    archiveClassifier = ""
    archiveVersion = project.version.toString()
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    dependencies {
        include(project(":core"))
    }
    relocate(corePackage, "by.overpas.detekt.${project.name}.core")
    manifest {
        attributes(
            "Implementation-Title" to archiveBaseName.get(),
            "Implementation-Version" to archiveVersion.get(),
        )
    }
}
