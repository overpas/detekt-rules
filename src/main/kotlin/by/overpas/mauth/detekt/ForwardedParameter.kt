package by.overpas.mauth.detekt

import com.intellij.psi.PsiElement
import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCatchClause
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.parents

class ForwardedParameter(config: Config) :
    Rule(
        config,
        "A function must not pass its own parameter to another function. " +
            "Pass a primitive or an object that the function creates, or call the function on the parameter.",
    ) {

    @Configuration("short names of the parameter types that a function can pass on")
    private val allowedTypes: Set<String> by config(
        listOf(
            "Int",
            "Long",
            "Short",
            "Byte",
            "Float",
            "Double",
            "Boolean",
            "Char",
            "String",
            "UInt",
            "ULong",
            "UShort",
            "UByte",
        ),
    ) { it.toSet() }

    @Configuration("short names of the annotations that mark a composable function")
    private val composableAnnotations: Set<String> by config(listOf("Composable")) { it.toSet() }

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        if (function.isExempt()) return
        val parameters = function.valueParameters
            .filter { it.typeReference?.shortTypeName() !in allowedTypes }
            .mapNotNull { it.name }
            .toSet()
        if (parameters.isEmpty()) return
        function.collectDescendantsOfType<KtCallExpression>()
            .filterNot { it.isConstructorCall() }
            .forEach { call ->
                call.valueArguments
                    .mapNotNull { it.getArgumentExpression() as? KtNameReferenceExpression }
                    .filter { it.getReferencedName() in parameters && it.resolvesTo(function) }
                    .forEach { report(Finding(Entity.from(it), it.findingMessage(function, call))) }
            }
    }

    private fun KtNamedFunction.isExempt(): Boolean =
        hasAnnotation(composableAnnotations) || hasModifier(KtTokens.OVERRIDE_KEYWORD)

    private fun KtCallExpression.isConstructorCall(): Boolean =
        calleeName()?.firstOrNull()?.isUpperCase() == true

    private fun KtNameReferenceExpression.resolvesTo(function: KtNamedFunction): Boolean {
        val name = getReferencedName()
        return parents
            .takeWhile { it != function }
            .none { name in it.declaredNames() }
    }

    private fun PsiElement.declaredNames(): Set<String> =
        when (this) {
            is KtBlockExpression -> statements.filterIsInstance<KtProperty>().mapNotNull { it.name }.toSet()
            is KtFunction -> valueParameters.mapNotNull { it.name }.toSet()
            is KtForExpression -> setOfNotNull(loopParameter?.name)
            is KtCatchClause -> setOfNotNull(catchParameter?.name)
            else -> emptySet()
        }

    private fun KtTypeReference.shortTypeName(): String? {
        val element = typeElement
        val named = if (element is KtNullableType) element.innerType else element
        return (named as? KtUserType)?.referencedName
    }

    private fun KtNameReferenceExpression.findingMessage(
        function: KtNamedFunction,
        call: KtCallExpression,
    ): String =
        "`${getReferencedName()}` is a parameter of `${function.name.orEmpty()}`. " +
            "Do not pass it to `${call.calleeExpression?.text.orEmpty()}`."
}
