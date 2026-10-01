import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    id("code-coverage")
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
    description = "Collects the versioned rule set jars into releases/<version>."
    from(ruleSetJars)
    into(layout.projectDirectory.dir("releases/$version"))
}
