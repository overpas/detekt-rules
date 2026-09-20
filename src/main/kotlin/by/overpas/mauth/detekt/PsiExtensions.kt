package by.overpas.mauth.detekt

import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtQualifiedExpression

internal fun KtNamedFunction.isUnitTest(testAnnotations: List<String>): Boolean = annotationEntries.any {
    it.shortName?.asString() in testAnnotations
}

internal fun KtCallExpression.isAssertion(assertionPrefixes: List<String>): Boolean {
    val name = calleeName() ?: return false
    return assertionPrefixes.any(name::startsWith)
}

internal fun KtExpression.isAssertion(assertionPrefixes: List<String>): Boolean =
    outermostCall()?.isAssertion(assertionPrefixes) == true

internal fun KtExpression.outermostCall(): KtCallExpression? = when (this) {
    is KtCallExpression -> this
    is KtQualifiedExpression -> selectorExpression?.outermostCall()
    else -> null
}

internal fun KtCallExpression.calleeName(): String? =
    (calleeExpression as? KtNameReferenceExpression)?.getReferencedName()

internal fun KtExpression.trailingLambdaBody(): KtBlockExpression? = outermostCall()
    ?.lambdaArguments
    ?.lastOrNull()
    ?.getLambdaExpression()
    ?.bodyExpression
