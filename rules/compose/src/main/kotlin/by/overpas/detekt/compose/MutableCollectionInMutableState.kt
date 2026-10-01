package by.overpas.detekt.compose

import by.overpas.detekt.core.calleeName
import by.overpas.detekt.core.outermostCall
import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtUserType

class MutableCollectionInMutableState(config: Config) :
    Rule(
        config,
        "Compose does not see an in-place change of a mutable collection in a MutableState. " +
            "Use mutableStateListOf or mutableStateMapOf, or put a read-only collection in the state.",
    ) {

    @Configuration("names of the calls that create a MutableState")
    private val stateFactories: Set<String> by config(listOf("mutableStateOf")) { it.toSet() }

    @Configuration("names of the calls that create a mutable collection")
    private val collectionFactories: Set<String> by config(MUTABLE_COLLECTION_FACTORIES) { it.toSet() }

    @Configuration("short names of the mutable collection types")
    private val collectionTypes: Set<String> by config(MUTABLE_COLLECTION_TYPES) { it.toSet() }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val name = expression.calleeName()?.takeIf { it in stateFactories } ?: return
        if (!expression.hasMutableTypeArgument() && !expression.hasMutableValue()) return
        report(
            Finding(
                Entity.from(expression),
                "Replace the mutable collection in `$name` " +
                    "with mutableStateListOf, mutableStateMapOf or a read-only collection.",
            ),
        )
    }

    private fun KtCallExpression.hasMutableTypeArgument(): Boolean =
        typeArguments.any { (it.typeReference?.typeElement as? KtUserType)?.referencedName in collectionTypes }

    private fun KtCallExpression.hasMutableValue(): Boolean =
        valueArguments
            .firstOrNull()
            ?.getArgumentExpression()
            ?.outermostCall()
            ?.calleeName() in collectionFactories
}
