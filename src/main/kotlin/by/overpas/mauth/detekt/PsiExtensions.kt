package by.overpas.mauth.detekt

import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.psiUtil.allChildren

internal const val MIN_BLOCK_COUNT = 2
internal const val MAX_BLOCK_COUNT = 3

internal fun KtNamedFunction.isUnitTest(testAnnotations: Set<String>): Boolean =
    annotationEntries.any {
        it.shortName?.asString() in testAnnotations
    }

internal fun KtCallExpression.isAssertion(assertionPrefixes: List<String>): Boolean {
    val name = calleeName() ?: return false
    return assertionPrefixes.any(name::startsWith)
}

internal fun KtExpression.isAssertion(assertionPrefixes: List<String>): Boolean =
    outermostCall()?.isAssertion(assertionPrefixes) == true

internal fun KtExpression.outermostCall(): KtCallExpression? =
    when (this) {
        is KtCallExpression -> this
        is KtQualifiedExpression -> selectorExpression?.outermostCall()
        else -> null
    }

internal fun KtCallExpression.calleeName(): String? =
    (calleeExpression as? KtNameReferenceExpression)?.getReferencedName()

internal fun KtExpression.trailingLambdaBody(): KtBlockExpression? =
    outermostCall()
        ?.lambdaArguments
        ?.lastOrNull()
        ?.getLambdaExpression()
        ?.bodyExpression

internal fun KtNamedFunction.testBody(assertionPrefixes: List<String>): KtBlockExpression? {
    val body = bodyBlockExpression ?: bodyExpression?.trailingLambdaBody()
    return body?.unwrapped(assertionPrefixes)
}

internal fun KtBlockExpression.blocks(): List<List<KtExpression>> {
    val statements = statements.toSet()
    val blocks = mutableListOf<MutableList<KtExpression>>()
    var isSeparated = true
    allChildren.forEach { child ->
        when (child) {
            is PsiWhiteSpace if child.isEmptyLine() -> {
                isSeparated = true
            }

            in statements -> {
                val statement = child as KtExpression
                if (isSeparated) blocks += mutableListOf(statement) else blocks.last() += statement
                isSeparated = false
            }
        }
    }
    return blocks
}

private fun KtBlockExpression.unwrapped(assertionPrefixes: List<String>): KtBlockExpression {
    val inner = statements.singleOrNull()
        ?.takeIf { !it.isAssertion(assertionPrefixes) }
        ?.trailingLambdaBody()
    return inner?.unwrapped(assertionPrefixes) ?: this
}

private fun PsiWhiteSpace.isEmptyLine(): Boolean =
    text.count { it == '\n' } > 1
