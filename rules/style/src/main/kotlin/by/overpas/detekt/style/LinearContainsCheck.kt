package by.overpas.detekt.style

import by.overpas.detekt.core.calleeName
import by.overpas.detekt.core.shortTypeName
import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtWhenConditionInRange

private const val CONTAINS = "contains"

class LinearContainsCheck(config: Config) :
    Rule(
        config,
        "A membership check on a list or an array runs in linear time. " +
            "A Set or a Map finds a value in constant time.",
    ) {

    @Configuration("short names of the types that do not find a value by a hash")
    private val linearTypes: Set<String> by config(
        listOf("List", "MutableList", "ArrayList", "LinkedList", "Array"),
    ) { it.toSet() }

    @Configuration("names of the calls that return a list or an array")
    private val linearFactories: Set<String> by config(
        listOf(
            "listOf",
            "listOfNotNull",
            "mutableListOf",
            "arrayListOf",
            "emptyList",
            "buildList",
            "arrayOf",
            "emptyArray",
            "arrayOfNulls",
            "asList",
            "toList",
            "toMutableList",
            "toTypedArray",
        ),
    ) { it.toSet() }

    private val linearity by lazy { Linearity(linearTypes, linearFactories) }

    override fun visitBinaryExpression(expression: KtBinaryExpression) {
        super.visitBinaryExpression(expression)
        val operation = expression.operationToken
        if (operation != KtTokens.IN_KEYWORD && operation != KtTokens.NOT_IN) return
        expression.right?.reportIfLinear()
    }

    override fun visitWhenConditionInRange(condition: KtWhenConditionInRange) {
        super.visitWhenConditionInRange(condition)
        condition.rangeExpression?.reportIfLinear()
    }

    override fun visitQualifiedExpression(expression: KtQualifiedExpression) {
        super.visitQualifiedExpression(expression)
        val call = expression.selectorExpression as? KtCallExpression ?: return
        if (call.calleeName() != CONTAINS || call.valueArguments.size != 1) return
        expression.receiverExpression.reportIfLinear()
    }

    private fun KtExpression.reportIfLinear() {
        if (isLinearContainer()) report(Finding(Entity.from(this), findingMessage()))
    }

    private fun KtExpression.isLinearContainer(): Boolean =
        when (this) {
            is KtNameReferenceExpression -> declaration()?.let { linearity.isLinear(it) } == true
            is KtQualifiedExpression -> selectorExpression?.isLinearContainer() == true
            else -> linearity.isFactoryCall(this)
        }

    private fun KtExpression.findingMessage(): String {
        val container = (this as? KtNameReferenceExpression)?.let { "`${it.getReferencedName()}`" }
            ?: "The data structure"
        return "$container finds a value in linear time. " +
            "Convert the data structure to a Set or a Map."
    }

    private class Linearity(
        private val types: Set<String>,
        factories: Set<String>,
    ) {

        private val factoryNames = factories + types

        fun isLinear(declaration: KtCallableDeclaration): Boolean =
            declaration.typeReference?.shortTypeName() in types ||
                (declaration as? KtProperty)?.initializer?.let { isFactoryCall(it) } == true

        fun isFactoryCall(expression: KtExpression): Boolean =
            when (expression) {
                is KtCallExpression -> expression.calleeName() in factoryNames
                is KtQualifiedExpression -> expression.selectorExpression?.let { isFactoryCall(it) } == true
                else -> false
            }
    }
}
