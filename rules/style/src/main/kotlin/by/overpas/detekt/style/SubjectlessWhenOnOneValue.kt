package by.overpas.detekt.style

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtIsExpression
import org.jetbrains.kotlin.psi.KtWhenCondition
import org.jetbrains.kotlin.psi.KtWhenConditionWithExpression
import org.jetbrains.kotlin.psi.KtWhenExpression

private const val MIN_BRANCH_COUNT = 2

private val SUBJECT_OPERATIONS = setOf(KtTokens.EQEQ, KtTokens.IN_KEYWORD, KtTokens.NOT_IN)

class SubjectlessWhenOnOneValue(config: Config) :
    Rule(
        config,
        "A subjectless when that tests one value in each branch hides the classified value. " +
            "Use when with a subject instead.",
    ) {

    override fun visitWhenExpression(expression: KtWhenExpression) {
        super.visitWhenExpression(expression)
        val subject = expression.commonSubject() ?: return
        report(Finding(Entity.from(expression), "Replace the subjectless when with `when ($subject)`."))
    }

    private fun KtWhenExpression.commonSubject(): String? {
        val branches = entries.filterNot { it.isElse }
        if (subjectExpression != null || branches.size < MIN_BRANCH_COUNT) return null
        return branches
            .asSequence()
            .flatMap { it.conditions.asSequence() }
            .map { BranchCondition(it).subjectText() }
            .distinct()
            .singleOrNull()
    }

    private class BranchCondition(private val condition: KtWhenCondition) {

        fun subjectText(): String? {
            val expression = (condition as? KtWhenConditionWithExpression)?.expression
            return expression?.testedExpression()?.text
        }

        private fun KtExpression.testedExpression(): KtExpression? =
            when (this) {
                is KtIsExpression -> leftHandSide
                is KtBinaryExpression -> left?.takeIf { operationToken in SUBJECT_OPERATIONS }
                else -> null
            }
    }
}
