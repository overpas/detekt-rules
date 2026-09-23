package by.overpas.mauth.detekt

import com.intellij.psi.PsiElement
import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.KtWhenConditionInRange
import org.jetbrains.kotlin.psi.psiUtil.parents

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

    private fun KtExpression.isLinearContainer(): Boolean = when (this) {
        is KtNameReferenceExpression -> declaration()?.isLinear() == true
        is KtQualifiedExpression -> selectorExpression?.isLinearContainer() == true
        else -> isLinearFactory()
    }

    private fun KtExpression.isLinearFactory(): Boolean = when (this) {
        is KtCallExpression -> calleeName().isLinearName()
        is KtQualifiedExpression -> selectorExpression?.isLinearFactory() == true
        else -> false
    }

    private fun String?.isLinearName(): Boolean = this in linearFactories || this in linearTypes

    private fun KtNameReferenceExpression.declaration(): KtCallableDeclaration? {
        val name = getReferencedName()
        return parents
            .flatMap { it.declarationsInScope() }
            .firstOrNull { it.name == name }
    }

    private fun PsiElement.declarationsInScope(): Sequence<KtCallableDeclaration> = when (this) {
        is KtBlockExpression -> statements.asSequence().filterIsInstance<KtProperty>()

        is KtFunction -> valueParameters.asSequence()

        is KtClassOrObject -> primaryConstructor?.valueParameters.orEmpty().asSequence() +
            declarations.asSequence().filterIsInstance<KtProperty>()

        is KtFile -> declarations.asSequence().filterIsInstance<KtProperty>()

        else -> emptySequence()
    }

    private fun KtCallableDeclaration.isLinear(): Boolean = typeReference?.shortTypeName() in linearTypes ||
        (this as? KtProperty)?.initializer?.isLinearFactory() == true

    private fun KtTypeReference.shortTypeName(): String? {
        val element = typeElement
        val named = if (element is KtNullableType) element.innerType else element
        return (named as? KtUserType)?.referencedName
    }

    private fun KtExpression.findingMessage(): String {
        val container = (this as? KtNameReferenceExpression)?.let { "`${it.getReferencedName()}`" }
            ?: "The data structure"
        return "$container finds a value in linear time. " +
            "Convert the data structure to a Set or a Map."
    }
}
