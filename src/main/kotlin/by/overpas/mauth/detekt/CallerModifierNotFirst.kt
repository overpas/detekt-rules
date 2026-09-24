package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

private const val THEN = "then"

class CallerModifierNotFirst(config: Config) :
    Rule(
        config,
        "The caller owns the placement of a composable. " +
            "Start the root modifier chain with the modifier parameter, then add the intrinsic modifiers.",
    ) {

    @Configuration("short names of the annotations that mark a composable function")
    private val composableAnnotations: Set<String> by config(listOf("Composable")) { it.toSet() }

    @Configuration("name of the modifier parameter")
    private val modifierName: String by config("modifier")

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        if (!function.hasAnnotation(composableAnnotations)) return
        if (function.valueParameters.none { it.name == modifierName }) return
        function.bodyExpression
            ?.collectDescendantsOfType<KtCallExpression> { it.isThenModifier() }
            .orEmpty()
            .forEach { call ->
                report(
                    Finding(
                        Entity.from(call),
                        "Start the chain with `$modifierName` instead of `then($modifierName)`.",
                    ),
                )
            }
    }

    private fun KtCallExpression.isThenModifier(): Boolean {
        if (calleeName() != THEN) return false
        val argument = valueArguments.singleOrNull()?.getArgumentExpression()
        return (argument as? KtNameReferenceExpression)?.getReferencedName() == modifierName
    }
}
