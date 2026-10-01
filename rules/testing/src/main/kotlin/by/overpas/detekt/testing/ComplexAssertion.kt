package by.overpas.detekt.testing

import by.overpas.detekt.core.calleeName
import by.overpas.detekt.core.outermostCall
import com.intellij.psi.PsiElement
import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

class ComplexAssertion(config: Config) :
    Rule(
        config,
        "A call or an object creation must not be embedded into an assertion. " +
            "An assertion accepts variables and literals only.",
    ) {

    @Configuration("short names of the annotations that mark a unit test")
    private val testAnnotations: Set<String> by config(listOf("Test")) { it.toSet() }

    @Configuration("name prefixes of the calls that count as an assertion")
    private val assertionPrefixes: List<String> by config(listOf("assert", "verify", "fail"))

    private val assertions by lazy { Assertions(assertionPrefixes) }

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        if (!function.isUnitTest(testAnnotations)) return
        function.collectDescendantsOfType<KtCallExpression> { assertions.isAssertion(it) }
            .flatMap { it.comparedExpressions() }
            .flatMap { it.embeddedCalls() }
            .forEach { report(Finding(Entity.from(it), it.findingMessage())) }
    }

    private fun KtCallExpression.comparedExpressions(): List<KtExpression> {
        val arguments = valueArguments
            .mapNotNull { it.getArgumentExpression() }
            .filter { it !is KtLambdaExpression }
        val receiver = (parent as? KtQualifiedExpression)
            ?.takeIf { it.selectorExpression == this }
            ?.receiverExpression
        return arguments + listOfNotNull(receiver)
    }

    private fun PsiElement.embeddedCalls(): List<KtExpression> =
        when {
            this is KtLambdaExpression -> emptyList()
            this is KtExpression && isForbiddenCall() -> listOf(this)
            else -> children.flatMap { it.embeddedCalls() }
        }

    private fun KtExpression.isForbiddenCall(): Boolean {
        val call = outermostCall() ?: return false
        return !assertions.isAssertion(call)
    }

    private fun KtExpression.findingMessage(): String {
        val call = outermostCall()?.calleeName()?.let { "`$it()`" } ?: "the call"
        return "Extract $call out of the assertion into a variable. " +
            "An assertion accepts variables and literals only."
    }
}
