import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    id("code-coverage")
}

val semVer = Regex(
    "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)" +
        "(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?" +
        "(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$",
)

check(semVer.matches(version.toString())) {
    "The version `$version` in gradle.properties is not a semantic version (MAJOR.MINOR.PATCH)."
}

val ruleSets = listOf("architecture", "compose", "coroutines", "gradle", "style", "testing")

val ruleSetJars by configurations.creating {
    isCanBeConsumed = false
    isTransitive = false
}

dependencies {
    kover(project(":core"))
    ruleSets.forEach { kover(project(":rules:$it")) }

    ruleSets.forEach { ruleSetJars(project(":rules:$it", "shadowRuntimeElements")) }
}

kover {
    reports {
        total {
            verify {
                rule {
                    minBound(80, CoverageUnit.LINE)
                }
            }
        }
    }
}

tasks.register<Sync>("ruleJars") {
    group = "build"
    description = "Collects the versioned rule set jars into build/rule-jars."
    from(ruleSetJars)
    into(layout.buildDirectory.dir("rule-jars"))
}
