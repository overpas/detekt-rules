package by.overpas.detekt.gradle

import by.overpas.detekt.core.calleeName
import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression

private const val GRADLE_SCRIPT_SUFFIX = ".gradle.kts"
private const val PROJECTS_PREFIX = "projects."
private const val PROJECT_CALL_PREFIX = "project("
private const val LIBS_PREFIX = "libs."
private const val PROJECT_GROUP = 0
private const val LIBRARY_GROUP = 1
private const val TEST_LIBRARY_GROUP = 2

private val groupThenName = compareBy<Pair<Int, String>> { it.first }.thenBy { it.second }

class GradleDeclarationOrder(config: Config) :
    Rule(
        config,
        "Plugins, source set dependency blocks and dependencies of a build script are sorted, " +
            "so a reader finds a declaration at once.",
    ) {

    @Configuration("version catalog aliases, after `libs.`, of the libraries only tests use")
    private val testLibraries: List<String> by config(
        listOf(
            "androidx.compose.ui.test",
            "androidx.espresso",
            "androidx.test",
            "compose.ui.test",
            "detekt.test",
            "junit",
            "kotlin.test",
            "kotlinx.coroutines.test",
            "robolectric",
        ),
    )

    private val blocks by lazy { ScriptBlocks(testLibraries) }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        if (!expression.containingKtFile.name.endsWith(GRADLE_SCRIPT_SUFFIX)) return
        val body = expression.lambdaBody() ?: return
        val findings = when (expression.calleeName()) {
            "plugins" -> blocks.plugins(body)
            "sourceSets" -> blocks.sourceSets(body)
            "dependencies" -> blocks.dependencies(body)
            else -> emptyList()
        }
        findings.forEach { report(it) }
    }

    private class ScriptBlocks(private val testLibraries: List<String>) {

        fun plugins(body: KtBlockExpression): List<Finding> =
            body.statements
                .mapNotNull { statement -> statement.pluginCall()?.let { statement to it.pluginKey() } }
                .unsorted(groupThenName)

        fun sourceSets(body: KtBlockExpression): List<Finding> =
            body.statements
                .mapNotNull { statement -> statement.dependencyBlockName()?.let { statement to it } }
                .unsorted(naturalOrder())

        fun dependencies(body: KtBlockExpression): List<Finding> {
            val declarations = body.statements.mapNotNull { it.dependencyDeclaration() }
            return declarations.splitConfigurations() +
                declarations
                    .groupBy { it.configuration }
                    .values
                    .flatMap { group ->
                        group
                            .map { it.statement to it.dependencyKey() }
                            .unsorted(groupThenName)
                    }
        }

        private fun List<DependencyDeclaration>.splitConfigurations(): List<Finding> {
            val closed = mutableSetOf<String>()
            return zipWithNext { previous, next ->
                if (previous.configuration != next.configuration) closed += previous.configuration
                if (next.configuration in closed) {
                    Finding(Entity.from(next.statement), "`${next.configuration}` declarations must stay together.")
                } else {
                    null
                }
            }.filterNotNull()
        }

        private fun <T> List<Pair<KtExpression, T>>.unsorted(comparator: Comparator<T>): List<Finding> =
            zipWithNext { (previous, previousKey), (next, nextKey) ->
                if (comparator.compare(nextKey, previousKey) < 0) {
                    Finding(Entity.from(next), "`${next.text}` must come before `${previous.text}`.")
                } else {
                    null
                }
            }.filterNotNull()

        private fun DependencyDeclaration.dependencyKey(): Pair<Int, String> {
            val group = when {
                notation.startsWith(PROJECTS_PREFIX) || notation.startsWith(PROJECT_CALL_PREFIX) -> PROJECT_GROUP

                notation.startsWith(LIBS_PREFIX) && notation.removePrefix(LIBS_PREFIX).isTestLibrary(testLibraries) ->
                    TEST_LIBRARY_GROUP

                else -> LIBRARY_GROUP
            }
            return group to notation
        }
    }
}
