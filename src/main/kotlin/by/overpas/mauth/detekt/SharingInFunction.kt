package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.psi.psiUtil.parents

class SharingInFunction(config: Config) :
    Rule(
        config,
        "Each call of a function or a getter starts one more sharing coroutine. " +
            "Expose stateIn and shareIn as one shared property.",
    ) {

    @Configuration("names of the calls that start a sharing coroutine")
    private val sharingCalls: Set<String> by config(listOf("stateIn", "shareIn", "stateInComponent")) { it.toSet() }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val name = expression.calleeName()?.takeIf { it in sharingCalls } ?: return
        val owner = expression.parents.firstOrNull {
            it is KtNamedFunction || it is KtPropertyAccessor || (it is KtProperty && !it.isLocal)
        }
        val isInFunction = owner is KtNamedFunction && !owner.isExtensionDeclaration()
        if (isInFunction || owner is KtPropertyAccessor) {
            report(
                Finding(
                    Entity.from(expression),
                    "Move `$name` into a property initializer so that all callers share one flow.",
                ),
            )
        }
    }
}
