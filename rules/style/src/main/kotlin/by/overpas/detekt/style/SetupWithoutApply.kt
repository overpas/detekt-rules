package by.overpas.detekt.style

import by.overpas.detekt.core.isConstructorCall
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
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtProperty

class SetupWithoutApply(config: Config) :
    Rule(
        config,
        "Many uses of a new object right after its creation repeat its name. " +
            "Configure the object in an apply block instead.",
    ) {

    @Configuration("minimum count of the consecutive uses of the object after its creation")
    private val minUseCount: Int by config(2)

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)
        val block = property.parent as? KtBlockExpression
        val name = property.name
        if (block == null || name == null || (property.initializer as? KtCallExpression)?.isConstructorCall() != true) {
            return
        }
        val setup = ObjectSetup(name, block.statements.dropWhile { it != property }.drop(1))
        if (!setup.isResultKept() && setup.useCount() >= minUseCount) {
            report(Finding(Entity.from(property), "Configure `$name` in an apply block."))
        }
    }

    private class ObjectSetup(
        private val objectName: String,
        private val statements: List<KtExpression>,
    ) {

        fun useCount(): Int =
            statements.takeWhile { it.isUse() }.size

        fun isResultKept(): Boolean =
            statements.any { statement ->
                val value = when (statement) {
                    is KtProperty -> statement.initializer
                    is KtBinaryExpression if statement.operationToken == KtTokens.EQ -> statement.right
                    else -> null
                }
                value.objectSelector() is KtCallExpression
            }

        private fun KtExpression.isUse(): Boolean =
            if (this is KtBinaryExpression) {
                operationToken in KtTokens.ALL_ASSIGNMENTS && left.objectSelector() != null
            } else {
                objectSelector() is KtCallExpression
            }

        private fun KtExpression?.objectSelector(): KtExpression? =
            (this as? KtDotQualifiedExpression)
                ?.takeIf { (it.receiverExpression as? KtNameReferenceExpression)?.getReferencedName() == objectName }
                ?.selectorExpression
    }
}
