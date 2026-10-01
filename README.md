# detekt-rules

Custom [detekt](https://detekt.dev) rules, split by category. Each category is one rule set and one
self-contained jar.

| Module | Rule set id | Jar |
|---|---|---|
| `rules:architecture` | `overpas-architecture` | `detekt-rules-architecture-<version>.jar` |
| `rules:compose` | `overpas-compose` | `detekt-rules-compose-<version>.jar` |
| `rules:coroutines` | `overpas-coroutines` | `detekt-rules-coroutines-<version>.jar` |
| `rules:gradle` | `overpas-gradle` | `detekt-rules-gradle-<version>.jar` |
| `rules:style` | `overpas-style` | `detekt-rules-style-<version>.jar` |
| `rules:testing` | `overpas-testing` | `detekt-rules-testing-<version>.jar` |

`core` holds the shared PSI helpers. It is not shipped alone: each rule set jar contains a relocated
copy, so any jar can be used without the others.

## Compatibility

The jars are built against detekt `2.0.0-alpha.6` and Kotlin `2.4.10`, with JVM target 17. The
consumer must run the same detekt version.

## Build

```shell
./gradlew build      # compile, test, detekt
./gradlew koverVerify
./gradlew ruleJars   # collects the versioned jars into build/rule-jars/
```

Set up the pre-commit hook once:

```shell
git config --local core.hooksPath config/git-hooks
```

## Use in a project

Copy the jars into the project, e.g. `config/detekt/plugins/`, and add them to the detekt plugins:

```kotlin
dependencies {
    detektPlugins(fileTree(rootProject.layout.projectDirectory.dir("config/detekt/plugins")) { include("*.jar") })
}
```

Configure the rules under their rule set ids in the detekt config. The options of each rule are in
`config/detekt/detekt.yml` of this repository. `RepeatedCollaboratorType` needs type resolution, so
configure it in the config of the type resolution tasks (see `config/detekt/detekt-type-resolution.yml`).

To upgrade, delete the old jars in the same change, so that two versions are never on the plugin
classpath. Stop the Gradle daemon (`./gradlew --stop`) after a jar change, because the daemon caches
the old plugin classes.

## Versioning

The project uses [Semantic Versioning 2.0.0](https://semver.org). One version applies to all jars.
The version is set in `gradle.properties`, and the build fails if it is not a semantic version.

- MAJOR: a rule or a rule set id is removed or renamed, a config option is removed or renamed, a
  default changes so that clean code starts to fail, or the detekt version changes.
- MINOR: a new rule or a new config option.
- PATCH: a false positive or a false negative is fixed, with no config change.

Until `1.0.0`, the API is not stable.

To release: set `version` in `gradle.properties`, add an entry to `CHANGELOG.md`, commit, and tag
the commit `v<version>`.
