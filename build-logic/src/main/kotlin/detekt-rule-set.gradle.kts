import org.gradle.accessors.dm.LibrariesForLibs
import java.util.zip.ZipFile

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

val verifyDefaultConfig by tasks.registering {
    group = "verification"
    description = "Checks that the rule set jar contains the default config."
    val jar = tasks.shadowJar.flatMap { it.archiveFile }
    inputs.file(jar)
    doLast {
        ZipFile(jar.get().asFile).use { zip ->
            check(zip.getEntry("config/config.yml") != null) {
                "${jar.get().asFile.name} does not contain config/config.yml"
            }
        }
    }
}

tasks.check {
    dependsOn(verifyDefaultConfig)
}
