package by.overpas.mauth.detekt

import com.intellij.psi.util.PsiTreeUtil
import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

class NonInjectedDependency(config: Config) :
    Rule(
        config,
        "A DI provider constructs only the dependency it provides. " +
            "Inject every other dependency as a parameter of the provider.",
    ) {

    @Configuration("short names of the annotations that mark a DI provider")
    private val providerAnnotations: Set<String> by config(listOf("Provides", "Binds")) { it.toSet() }

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        if (!function.hasAnnotation(providerAnnotations)) return
        function.bodyExpression?.let(::check)
    }

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)
        if (!property.hasAnnotation(providerAnnotations)) return
        property.initializer?.let(::check)
        property.getter?.bodyExpression?.let(::check)
    }

    private fun check(body: KtExpression) {
        val provided = body.providedCalls()
        val implementations = provided.flatMap { it.lambdaArguments }
        body.collectDescendantsOfType<KtCallExpression> { call ->
            call.isConstructorCall() &&
                call !in provided &&
                implementations.none { PsiTreeUtil.isAncestor(it, call, true) }
        }
            .forEach { call ->
                report(
                    Finding(
                        Entity.from(call),
                        "Inject `${call.calleeName().orEmpty()}` as a parameter of the provider " +
                            "instead of constructing it.",
                    ),
                )
            }
    }

    private fun KtExpression.providedCalls(): List<KtCallExpression> =
        if (this is KtBlockExpression) {
            collectDescendantsOfType<KtReturnExpression>()
                .mapNotNull { it.returnedExpression?.outermostCall() }
        } else {
            listOfNotNull(outermostCall())
        }

    private fun KtCallExpression.isConstructorCall(): Boolean =
        calleeName()?.firstOrNull()?.isUpperCase() == true
}
