package by.overpas.detekt.coroutines

import by.overpas.detekt.core.calleeName
import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

private const val RUN_BLOCKING = "runBlocking"

class RunBlockingOutsideMain(config: Config) :
    Rule(
        config,
        "runBlocking blocks a thread and hides the cancellation. " +
            "Use it only at a true blocking edge such as main. Use suspend functions or runTest instead.",
    ) {

    @Configuration("names of the top-level functions that can call runBlocking")
    private val allowedFunctions: Set<String> by config(listOf("main")) { it.toSet() }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        if (expression.calleeName() != RUN_BLOCKING) return
        val function = expression.getStrictParentOfType<KtNamedFunction>()
        if (function != null && function.isTopLevel && function.name in allowedFunctions) return
        report(
            Finding(
                Entity.from(expression),
                "Remove runBlocking. Make the function suspend and let its caller own the coroutine.",
            ),
        )
    }
}
