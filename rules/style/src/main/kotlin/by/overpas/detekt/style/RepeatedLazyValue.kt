package by.overpas.detekt.style

import by.overpas.detekt.core.shortTypeName
import com.intellij.psi.PsiElement
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

class RepeatedLazyValue(config: Config) :
    Rule(
        config,
        "The `value` of a `Lazy` must be read only once. " +
            "Delegate a property to the `Lazy` with `by`, or keep the value in one private property.",
    ) {

    override fun visitParameter(parameter: KtParameter) {
        super.visitParameter(parameter)
        val owner = parameter.ownerFunction
        check(parameter, if (owner is KtPrimaryConstructor) owner.getContainingClassOrObject() else owner)
    }

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)
        check(
            property,
            if (property.isLocal) property.parent else property.containingClassOrObject ?: property.containingKtFile,
        )
    }

    private fun check(
        declaration: KtCallableDeclaration,
        scope: PsiElement?,
    ) {
        val name = declaration.name
        if (scope == null || name == null || declaration.typeReference?.shortTypeName() != "Lazy") return
        val reads = scope.collectDescendantsOfType<KtQualifiedExpression>(
            canGoInside = { it == scope || !it.declares(name) },
        ) { expression ->
            val receiver = expression.receiverExpression
            val target = (receiver as? KtQualifiedExpression)
                ?.takeIf { it.receiverExpression is KtThisExpression }
                ?.selectorExpression
                ?: receiver
            expression.selectorExpression?.text == "value" && target.text == name
        }
        if (reads.size > 1) {
            report(
                Finding(
                    Entity.from(declaration),
                    "`$name.value` is read ${reads.size} times. " +
                        "Delegate a property to `$name` with `by`, or keep the value in one private property.",
                ),
            )
        }
    }

    private fun PsiElement.declares(name: String): Boolean =
        when (this) {
            is KtClassOrObject -> primaryConstructorParameters.any { it.name == name } ||
                declarations.any { it is KtProperty && it.name == name }

            is KtFunction -> valueParameters.any { it.name == name }

            is KtBlockExpression -> statements.any { it is KtProperty && it.name == name }

            else -> false
        }
}
