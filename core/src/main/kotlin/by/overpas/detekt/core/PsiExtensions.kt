package by.overpas.detekt.core

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.psiUtil.parents

fun KtAnnotated.hasAnnotation(names: Set<String>): Boolean =
    annotationEntries.any {
        it.shortName?.asString() in names
    }

fun PsiElement.isDeferredWithin(root: PsiElement): Boolean =
    parents
        .takeWhile { it != root }
        .any { it is KtFunction }

fun KtExpression.outermostCall(): KtCallExpression? =
    when (this) {
        is KtCallExpression -> this
        is KtQualifiedExpression -> selectorExpression?.outermostCall()
        else -> null
    }

fun KtCallExpression.calleeName(): String? =
    (calleeExpression as? KtNameReferenceExpression)?.getReferencedName()

fun KtExpression.trailingLambdaBody(): KtBlockExpression? =
    outermostCall()
        ?.lambdaArguments
        ?.lastOrNull()
        ?.getLambdaExpression()
        ?.bodyExpression
