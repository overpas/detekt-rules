package by.overpas.detekt.testing

import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.allChildren

internal const val MIN_BLOCK_COUNT = 2
internal const val MAX_BLOCK_COUNT = 3

internal fun KtNamedFunction.isUnitTest(testAnnotations: Set<String>): Boolean =
    annotationEntries.any {
        it.shortName?.asString() in testAnnotations
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

private fun PsiWhiteSpace.isEmptyLine(): Boolean =
    text.count { it == '\n' } > 1
