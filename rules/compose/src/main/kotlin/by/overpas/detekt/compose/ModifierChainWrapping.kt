package by.overpas.detekt.compose

import com.intellij.psi.PsiWhiteSpace
import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression

class ModifierChainWrapping(config: Config) :
    Rule(
        config,
        "A long modifier chain on one line is hard to scan. Put each call of the chain on a new line.",
    ) {

    @Configuration("names of the expressions that start a modifier chain")
    private val chainStarts: Set<String> by config(listOf("Modifier", "modifier")) { it.toSet() }

    @Configuration("minimum number of calls in a chain that must be one per line")
    private val minCallCount: Int by config(3)

    override fun visitQualifiedExpression(expression: KtQualifiedExpression) {
        super.visitQualifiedExpression(expression)
        if (!expression.isChainEnd()) return
        val chain = Chain(expression)
        if (chain.startsWith(chainStarts) && chain.hasUnwrappedCalls(minCallCount)) {
            report(Finding(Entity.from(expression), "Put each call of the modifier chain on a new line."))
        }
    }

    private fun KtQualifiedExpression.isChainEnd(): Boolean =
        (parent as? KtQualifiedExpression)?.receiverExpression != this

    private class Chain(end: KtQualifiedExpression) {

        private val links = generateSequence(end) { it.receiverExpression as? KtQualifiedExpression }.toList()

        fun startsWith(names: Set<String>): Boolean =
            (links.last().receiverExpression as? KtNameReferenceExpression)?.getReferencedName() in names

        fun hasUnwrappedCalls(minCallCount: Int): Boolean =
            links.count { it.selectorExpression is KtCallExpression } >= minCallCount && !links.all { it.isWrapped() }

        private fun KtQualifiedExpression.isWrapped(): Boolean {
            val whitespace = operationTokenNode.psi.prevSibling as? PsiWhiteSpace
            return whitespace != null && '\n' in whitespace.text
        }
    }
}
