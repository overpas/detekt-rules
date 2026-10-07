package by.overpas.detekt.testing

import by.overpas.detekt.core.calleeName
import by.overpas.detekt.core.outermostCall
import by.overpas.detekt.core.trailingLambdaBody
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtQualifiedExpression

internal class Assertions(private val prefixes: List<String>) {

    fun isAssertion(expression: KtExpression): Boolean {
        val name = expression.outermostCall()?.calleeName() ?: return false
        return prefixes.any(name::startsWith)
    }

    fun testBody(function: KtNamedFunction): KtBlockExpression? {
        val body = function.bodyBlockExpression ?: function.bodyExpression?.trailingLambdaBody()
        return body?.let(::unwrapped)
    }

    fun comparedExpressions(assertion: KtCallExpression): List<KtExpression> {
        val arguments = assertion.valueArguments
            .mapNotNull { it.getArgumentExpression() }
            .filter { it !is KtLambdaExpression }
        val receiver = (assertion.parent as? KtQualifiedExpression)
            ?.takeIf { it.selectorExpression == assertion }
            ?.receiverExpression
        return arguments + listOfNotNull(receiver)
    }

    private fun unwrapped(block: KtBlockExpression): KtBlockExpression {
        val inner = block.statements.singleOrNull()
            ?.takeIf { !isAssertion(it) }
            ?.trailingLambdaBody()
        return inner?.let(::unwrapped) ?: block
    }
}
