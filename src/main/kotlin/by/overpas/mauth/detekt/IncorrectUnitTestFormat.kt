package by.overpas.mauth.detekt

import com.intellij.psi.PsiWhiteSpace
import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.allChildren

private const val MIN_BLOCK_COUNT = 2
private const val MAX_BLOCK_COUNT = 3

class IncorrectUnitTestFormat(config: Config) :
    Rule(
        config,
        "A unit test body must be separated by empty lines into an optional arrange block, " +
            "an act block and a final assert block.",
    ) {

    @Configuration("short names of the annotations that mark a unit test")
    private val testAnnotations: List<String> by config(listOf("Test"))

    @Configuration("name prefixes of the calls that count as an assertion")
    private val assertionPrefixes: List<String> by config(listOf("assert", "verify", "fail"))

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        if (!function.isUnitTest()) return
        function.testBody()
            ?.blocks()
            ?.formatError()
            ?.let { report(Finding(Entity.atName(function), it)) }
    }

    private fun KtNamedFunction.isUnitTest(): Boolean = annotationEntries.any {
        it.shortName?.asString() in testAnnotations
    }

    private fun KtNamedFunction.testBody(): KtBlockExpression? {
        val body = bodyBlockExpression ?: bodyExpression?.trailingLambdaBody()
        return body?.unwrapped()
    }

    private fun KtBlockExpression.unwrapped(): KtBlockExpression {
        val inner = statements.singleOrNull()
            ?.takeIf { !it.isAssertion() }
            ?.trailingLambdaBody()
        return inner?.unwrapped() ?: this
    }

    private fun KtBlockExpression.blocks(): List<List<KtExpression>> {
        val statements = statements.toSet()
        val blocks = mutableListOf<MutableList<KtExpression>>()
        var separated = true
        allChildren.forEach { child ->
            when {
                child is PsiWhiteSpace && child.isEmptyLine() -> {
                    separated = true
                }

                child in statements -> {
                    val statement = child as KtExpression
                    if (separated) blocks += mutableListOf(statement) else blocks.last() += statement
                    separated = false
                }
            }
        }
        return blocks
    }

    private fun List<List<KtExpression>>.formatError(): String? = when {
        size < MIN_BLOCK_COUNT ->
            "An act block and an assert block must be separated by an empty line."

        size > MAX_BLOCK_COUNT ->
            "There must be no more than $MAX_BLOCK_COUNT blocks: arrange, act and assert."

        last().any { !it.isAssertion() } ->
            "The last block must contain assertions only."

        dropLast(1).any { block -> block.any { it.isAssertion() } } ->
            "An assertion is only allowed in the last block."

        else -> null
    }

    private fun KtExpression.isAssertion(): Boolean {
        val name = outermostCall()?.calleeName() ?: return false
        return assertionPrefixes.any(name::startsWith)
    }
}

private fun PsiWhiteSpace.isEmptyLine(): Boolean = text.count { it == '\n' } > 1

private fun KtExpression.outermostCall(): KtCallExpression? = when (this) {
    is KtCallExpression -> this
    is KtDotQualifiedExpression -> selectorExpression?.outermostCall()
    else -> null
}

private fun KtCallExpression.calleeName(): String? =
    (calleeExpression as? KtNameReferenceExpression)?.getReferencedName()

private fun KtExpression.trailingLambdaBody(): KtBlockExpression? = outermostCall()
    ?.lambdaArguments
    ?.lastOrNull()
    ?.getLambdaExpression()
    ?.bodyExpression
