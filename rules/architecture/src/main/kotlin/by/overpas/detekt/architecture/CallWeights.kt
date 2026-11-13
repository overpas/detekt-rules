package by.overpas.detekt.architecture

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtLoopExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtObjectLiteralExpression
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.KtWhileExpressionBase

internal class CallWeights private constructor(
    private val nest: Int,
    private val weights: MutableList<Double>,
) : KtTreeVisitorVoid() {

    override fun visitExpression(expression: KtExpression) {
        if (expression.isCall()) {
            weights += 1 + NEST_WEIGHT * nest + CHAIN_WEIGHT * expression.chainLength()
        }
        super.visitExpression(expression)
    }

    override fun visitIfExpression(expression: KtIfExpression) {
        expression.condition?.accept(this)
        expression.then.acceptDeeper()
        val otherwise = expression.`else`
        if (otherwise is KtIfExpression) {
            otherwise.accept(this)
        } else {
            otherwise.acceptDeeper()
        }
    }

    override fun visitWhenEntry(whenEntry: KtWhenEntry) {
        whenEntry.conditions.forEach { it.accept(this) }
        whenEntry.guard?.accept(this)
        whenEntry.expression.acceptDeeper()
    }

    override fun visitLoopExpression(loopExpression: KtLoopExpression) {
        (loopExpression as? KtForExpression)?.loopRange?.accept(this)
        (loopExpression as? KtWhileExpressionBase)?.condition?.accept(this)
        loopExpression.body.acceptDeeper()
    }

    override fun visitTryExpression(expression: KtTryExpression) {
        expression.tryBlock.acceptDeeper()
        expression.catchClauses.forEach { it.catchBody.acceptDeeper() }
        expression.finallyBlock?.finalExpression.acceptDeeper()
    }

    override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
        lambdaExpression.bodyExpression.acceptDeeper()
    }

    override fun visitObjectLiteralExpression(expression: KtObjectLiteralExpression) {
        expression.objectDeclaration.acceptDeeper()
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        if (function.isLocal) {
            function.valueParameterList?.accept(this)
            function.bodyExpression.acceptDeeper()
        } else {
            super.visitNamedFunction(function)
        }
    }

    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
        if (classOrObject is KtObjectDeclaration && classOrObject.isObjectLiteral()) {
            super.visitClassOrObject(classOrObject)
        }
    }

    override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) =
        Unit

    private fun KtElement?.acceptDeeper() {
        this?.accept(CallWeights(nest + 1, weights))
    }

    private fun KtExpression.isCall(): Boolean =
        when (this) {
            is KtCallExpression, is KtArrayAccessExpression, is KtPrefixExpression -> true
            is KtBinaryExpression -> operationToken !in nonCallOperators
            is KtPostfixExpression -> operationToken != KtTokens.EXCLEXCL
            else -> false
        }

    private fun KtExpression.chainLength(): Int =
        (this as? KtArrayAccessExpression)?.arrayExpression.segments() + earlierSegments()

    private fun KtExpression.earlierSegments(): Int {
        val parent = parent
        return when {
            parent is KtQualifiedExpression && parent.selectorExpression == this ->
                parent.receiverExpression.segments()

            parent is KtArrayAccessExpression && parent.arrayExpression == this -> parent.earlierSegments()

            else -> 0
        }
    }

    private fun KtExpression?.segments(): Int =
        when (this) {
            is KtQualifiedExpression -> receiverExpression.segments() + selectorExpression.segments()
            is KtArrayAccessExpression -> 1 + arrayExpression.segments()
            is KtPostfixExpression -> baseExpression.segments()
            is KtCallExpression -> 1
            else -> 0
        }

    companion object {

        private const val NEST_WEIGHT = 0.25
        private const val CHAIN_WEIGHT = 0.1

        private val nonCallOperators: Set<IElementType> = setOf(KtTokens.EQ, KtTokens.EQEQEQ, KtTokens.EXCLEQEQEQ)

        fun total(element: KtElement): Double =
            mutableListOf<Double>()
                .also { element.accept(CallWeights(0, it)) }
                .sum()
    }
}
