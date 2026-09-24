package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class StoredCoroutineScope(config: Config) :
    Rule(
        config,
        "A stored CoroutineScope hides who owns the work, and a cancelled scope silently drops later launches. " +
            "Expose suspend functions and let the lifecycle owner launch them.",
    ) {

    @Configuration("short names of the coroutine scope types")
    private val scopeTypes: Set<String> by config(listOf("CoroutineScope")) { it.toSet() }

    @Configuration("names of the calls that create a coroutine scope")
    private val scopeFactories: Set<String> by config(listOf("CoroutineScope", "MainScope")) { it.toSet() }

    @Configuration("short names of the classes that own a coroutine scope as lifecycle infrastructure")
    private val allowedClasses: Set<String> by config(emptyList<String>()) { it.toSet() }

    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
        super.visitClassOrObject(classOrObject)
        if (classOrObject.isAllowed()) return
        classOrObject.superTypeListEntries
            .filter { it.typeReference.isScopeType() }
            .forEach { report(it, "`${classOrObject.nameAsSafeName}` is a CoroutineScope") }
    }

    override fun visitParameter(parameter: KtParameter) {
        super.visitParameter(parameter)
        val constructor = (parameter.parent as? KtParameterList)?.parent as? KtPrimaryConstructor
        val owner = constructor?.getContainingClassOrObject()
        if (owner != null && !owner.isAllowed() && parameter.typeReference.isScopeType()) {
            report(parameter, "`${owner.nameAsSafeName}` receives the scope `${parameter.nameAsSafeName}`")
        }
    }

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)
        val isStored = !property.isLocal && (property.hasInitializer() || property.hasDelegate())
        val isAllowed = property.getStrictParentOfType<KtClassOrObject>()?.isAllowed() == true
        if (isStored && !isAllowed && property.holdsScope()) {
            report(property, "`${property.nameAsSafeName}` stores a scope")
        }
    }

    private fun KtProperty.holdsScope(): Boolean =
        typeReference.isScopeType() ||
            initializer?.createsScope() == true ||
            delegateExpression?.lazyResult()?.createsScope() == true

    private fun KtExpression.createsScope(): Boolean =
        outermostCall()?.calleeName() in scopeFactories

    private fun KtExpression.lazyResult(): KtExpression? =
        trailingLambdaBody()
            ?.statements
            .orEmpty()
            .lastOrNull()

    private fun KtTypeReference?.isScopeType(): Boolean {
        val element = this?.typeElement
        val type = if (element is KtNullableType) element.innerType else element
        return (type as? KtUserType)?.referencedName in scopeTypes
    }

    private fun KtClassOrObject.isAllowed(): Boolean =
        name in allowedClasses

    private fun report(
        element: KtElement,
        subject: String,
    ) {
        report(Finding(Entity.from(element), "$subject. Replace the stored scope with suspend functions."))
    }
}
