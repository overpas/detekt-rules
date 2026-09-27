package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

private const val GRADLE_SCRIPT_SUFFIX = ".gradle.kts"
private const val PLUGIN_ALIAS = "alias"
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
            "ultron",
        ),
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        if (!expression.containingKtFile.name.endsWith(GRADLE_SCRIPT_SUFFIX)) return
        val body = expression.lambdaBody() ?: return
        when (expression.calleeName()) {
            "plugins" -> checkPlugins(body)
            "sourceSets" -> checkSourceSets(body)
            "dependencies" -> checkDependencies(body)
        }
    }

    private fun checkPlugins(body: KtBlockExpression) {
        body.statements
            .mapNotNull { statement -> statement.pluginCall()?.let { statement to it.pluginKey() } }
            .reportUnsorted(groupThenName)
    }

    private fun checkSourceSets(body: KtBlockExpression) {
        body.statements
            .mapNotNull { statement -> statement.dependencyBlockName()?.let { statement to it } }
            .reportUnsorted(naturalOrder())
    }

    private fun checkDependencies(body: KtBlockExpression) {
        val declarations = body.statements.mapNotNull { it.dependencyDeclaration() }
        reportSplitConfigurations(declarations)
        declarations
            .groupBy { it.configuration }
            .values
            .forEach { group ->
                group
                    .map { it.statement to it.dependencyKey() }
                    .reportUnsorted(groupThenName)
            }
    }

    private fun reportSplitConfigurations(declarations: List<DependencyDeclaration>) {
        val closed = mutableSetOf<String>()
        declarations.zipWithNext { previous, next ->
            if (previous.configuration != next.configuration) closed += previous.configuration
            if (next.configuration in closed) {
                report(
                    Finding(
                        Entity.from(next.statement),
                        "`${next.configuration}` declarations must stay together.",
                    ),
                )
            }
        }
    }

    private fun <T> List<Pair<KtExpression, T>>.reportUnsorted(comparator: Comparator<T>) {
        zipWithNext { (previous, previousKey), (next, nextKey) ->
            if (comparator.compare(nextKey, previousKey) < 0) {
                report(
                    Finding(
                        Entity.from(next),
                        "`${next.text}` must come before `${previous.text}`.",
                    ),
                )
            }
        }
    }

    private fun DependencyDeclaration.dependencyKey(): Pair<Int, String> {
        val group = when {
            notation.startsWith(PROJECTS_PREFIX) || notation.startsWith(PROJECT_CALL_PREFIX) -> PROJECT_GROUP

            notation.startsWith(LIBS_PREFIX) && notation.removePrefix(LIBS_PREFIX).isTestLibrary() ->
                TEST_LIBRARY_GROUP

            else -> LIBRARY_GROUP
        }
        return group to notation
    }

    private fun String.isTestLibrary(): Boolean =
        testLibraries.any { this == it || startsWith("$it.") }

    private fun KtExpression.dependencyDeclaration(): DependencyDeclaration? {
        val configuration = (this as? KtCallExpression)
            ?.takeIf { it.lambdaArguments.isEmpty() }
            ?.calleeName()
        return configuration?.let {
            DependencyDeclaration(this, it, (this as KtCallExpression).firstArgumentText())
        }
    }

    private data class DependencyDeclaration(
        val statement: KtExpression,
        val configuration: String,
        val notation: String,
    )
}

private fun KtCallExpression.lambdaBody(): KtBlockExpression? =
    lambdaArguments.singleOrNull()?.getLambdaExpression()?.bodyExpression

private fun KtExpression.pluginCall(): KtCallExpression? =
    when (this) {
        is KtBinaryExpression -> left?.pluginCall()
        is KtCallExpression -> this
        else -> null
    }

private fun KtCallExpression.pluginKey(): Pair<Int, String> {
    val group = if (calleeName() == PLUGIN_ALIAS) 1 else 0
    return group to firstArgumentText()
}

private fun KtExpression.dependencyBlockName(): String? {
    val selector = (this as? KtDotQualifiedExpression)?.selectorExpression as? KtCallExpression
    if (selector?.calleeName() != "dependencies") return null
    return when (val receiver = (this as KtDotQualifiedExpression).receiverExpression) {
        is KtNameReferenceExpression -> receiver.getReferencedName()
        is KtCallExpression -> receiver.firstArgumentText()
        else -> null
    }
}

private fun KtCallExpression.firstArgumentText(): String =
    valueArguments
        .firstOrNull()
        ?.getArgumentExpression()
        ?.text
        .orEmpty()
        .trim('"')
