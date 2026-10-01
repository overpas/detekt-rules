package by.overpas.detekt.compose

import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class ReturnInComposable(config: Config) :
    Rule(
        config,
        "A composable function that returns Unit must not contain return statements. Use if or when instead.",
    ) {

    @Configuration("short names of the annotations that mark a composable function")
    private val composableAnnotations: Set<String> by config(listOf("Composable")) { it.toSet() }

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        if (!function.isUnitComposable()) return
        function.bodyExpression
            ?.collectDescendantsOfType<KtReturnExpression> { it.getStrictParentOfType<KtNamedFunction>() == function }
            .orEmpty()
            .forEach { returnExpression ->
                report(
                    Finding(
                        Entity.from(returnExpression),
                        "Remove the return statement from the composable `${function.nameAsSafeName}`. " +
                            "Use if or when instead.",
                    ),
                )
            }
    }

    private fun KtNamedFunction.isUnitComposable(): Boolean {
        val isComposable = annotationEntries.any { it.shortName?.asString() in composableAnnotations }
        val returnType = typeReference?.text
        val hasUnitReturnType = returnType == "Unit" || (returnType == null && hasBlockBody())
        return isComposable && hasUnitReturnType
    }
}
