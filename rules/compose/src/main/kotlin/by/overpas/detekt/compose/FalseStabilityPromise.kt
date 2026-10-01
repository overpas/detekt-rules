package by.overpas.detekt.compose

import by.overpas.detekt.core.calleeName
import by.overpas.detekt.core.hasAnnotation
import by.overpas.detekt.core.outermostCall
import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtUserType

private const val SNAPSHOT_STATE_PREFIX = "mutable"
private const val SNAPSHOT_STATE_SUFFIX = "StateOf"

class FalseStabilityPromise(config: Config) :
    Rule(
        config,
        "A false stability promise lets Compose skip a recomposition and show stale UI. " +
            "An immutable class has only read-only properties. " +
            "A stable class changes only through snapshot state.",
    ) {

    @Configuration("short names of the annotations that promise immutability")
    private val immutableAnnotations: Set<String> by config(listOf("Immutable")) { it.toSet() }

    @Configuration("short names of the annotations that promise stability")
    private val stableAnnotations: Set<String> by config(listOf("Stable")) { it.toSet() }

    @Configuration("short names of the mutable collection types")
    private val collectionTypes: Set<String> by config(MUTABLE_COLLECTION_TYPES) { it.toSet() }

    @Configuration("names of the calls that create a mutable collection")
    private val collectionFactories: Set<String> by config(MUTABLE_COLLECTION_FACTORIES) { it.toSet() }

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)
        val isImmutable = klass.hasAnnotation(immutableAnnotations)
        if (!isImmutable && !klass.hasAnnotation(stableAnnotations)) return
        val constructorProperties = klass.primaryConstructorParameters.filter { it.hasValOrVar() }
        val bodyProperties = klass.getProperties()
        (constructorProperties + bodyProperties)
            .filter { it.isMutableCollection() || (it.isVar() && (isImmutable || !it.isSnapshotState())) }
            .forEach { property ->
                report(
                    Finding(
                        Entity.from(property),
                        "Make `${property.nameAsSafeName}` read-only and immutable, " +
                            "or remove the stability annotation from `${klass.nameAsSafeName}`.",
                    ),
                )
            }
    }

    private fun KtCallableDeclaration.isVar(): Boolean =
        when (this) {
            is KtProperty -> isVar
            is KtParameter -> isMutable
            else -> false
        }

    private fun KtCallableDeclaration.isSnapshotState(): Boolean {
        val name = (this as? KtProperty)?.delegateExpression?.outermostCall()?.calleeName() ?: return false
        return name.startsWith(SNAPSHOT_STATE_PREFIX) && name.endsWith(SNAPSHOT_STATE_SUFFIX)
    }

    private fun KtCallableDeclaration.isMutableCollection(): Boolean {
        val element = typeReference?.typeElement
        val type = if (element is KtNullableType) element.innerType else element
        val initializer: KtExpression? = (this as? KtProperty)?.initializer
        return (type as? KtUserType)?.referencedName in collectionTypes ||
            initializer?.outermostCall()?.calleeName() in collectionFactories
    }
}
