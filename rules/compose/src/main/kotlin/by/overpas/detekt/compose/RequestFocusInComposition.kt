package by.overpas.detekt.compose

import by.overpas.detekt.core.calleeName
import by.overpas.detekt.core.hasAnnotation
import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

private const val REQUEST_FOCUS = "requestFocus"

class RequestFocusInComposition(config: Config) :
    Rule(
        config,
        "A focus request in the composable body runs on each recomposition. " +
            "Request the focus from LaunchedEffect or from an event callback.",
    ) {

    @Configuration("short names of the annotations that mark a composable function")
    private val composableAnnotations: Set<String> by config(listOf("Composable")) { it.toSet() }

    @Configuration("names of the composable calls whose trailing lambda is not composable content")
    private val effectCalls: Set<String> by config(
        listOf("LaunchedEffect", "DisposableEffect", "SideEffect"),
    ) { it.toSet() }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        if (expression.calleeName() != REQUEST_FOCUS || !expression.isInComposition()) return
        report(
            Finding(
                Entity.from(expression),
                "Move `requestFocus()` into a LaunchedEffect keyed to the condition that makes the target present.",
            ),
        )
    }

    private fun KtCallExpression.isInComposition(): Boolean {
        val owner = generateSequence(getStrictParentOfType<KtFunction>()) { it.getStrictParentOfType<KtFunction>() }
            .firstOrNull { it !is KtFunctionLiteral || !it.isComposableContent() }
        return owner is KtNamedFunction && owner.hasAnnotation(composableAnnotations)
    }

    private fun KtFunctionLiteral.isComposableContent(): Boolean {
        val argument = (parent as? KtLambdaExpression)?.parent as? KtLambdaArgument
        val name = (argument?.parent as? KtCallExpression)?.calleeName() ?: return false
        return name.first().isUpperCase() && name !in effectCalls
    }
}
