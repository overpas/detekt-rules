package by.overpas.mauth.detekt

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.parents

internal const val MIN_BLOCK_COUNT = 2
internal const val MAX_BLOCK_COUNT = 3

internal val MUTABLE_COLLECTION_TYPES = listOf(
    "MutableList",
    "MutableSet",
    "MutableMap",
    "MutableCollection",
    "ArrayList",
    "HashSet",
    "HashMap",
    "LinkedHashSet",
    "LinkedHashMap",
)

internal val MUTABLE_COLLECTION_FACTORIES = listOf(
    "mutableListOf",
    "mutableSetOf",
    "mutableMapOf",
    "arrayListOf",
    "hashSetOf",
    "hashMapOf",
    "linkedSetOf",
    "linkedMapOf",
    "toMutableList",
    "toMutableSet",
    "toMutableMap",
    "ArrayList",
    "HashSet",
    "HashMap",
    "LinkedHashSet",
    "LinkedHashMap",
)

internal fun KtNamedFunction.isUnitTest(testAnnotations: Set<String>): Boolean =
    annotationEntries.any {
        it.shortName?.asString() in testAnnotations
    }

internal fun KtAnnotated.hasAnnotation(names: Set<String>): Boolean =
    annotationEntries.any {
        it.shortName?.asString() in names
    }

internal fun PsiElement.isDeferredWithin(root: PsiElement): Boolean =
    parents
        .takeWhile { it != root }
        .any { it is KtFunction }

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

private fun PsiWhiteSpace.isEmptyLine(): Boolean =
    text.count { it == '\n' } > 1
