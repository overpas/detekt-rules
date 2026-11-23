package by.overpas.detekt.architecture

import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.isSubpackageOf
import org.jetbrains.kotlin.psi.KtFile

class StoreTypeLeak(config: Config) :
    Rule(
        config,
        "The nested types of a store must stay near the store. Intent, Action and Message stay in the package " +
            "of the store and its allowed subpackages. The component can also use Intent, State and Label, " +
            "and the UI can use State and Label.",
    ) {

    @Configuration("suffix of the names of the store types")
    private val storeSuffix: String by config("Store")

    @Configuration("names of the nested types of a store that the rule checks")
    private val storeTypes: Set<String> by config(
        listOf(INTENT, "Action", "Message", STATE, LABEL),
    ) { it.toSet() }

    @Configuration("subpackages of the store package that can use all the nested types of the store")
    private val allowedSubpackages: List<String> by config(emptyList())

    @Configuration("nested types of a store that the component package can also use")
    private val componentTypes: Set<String> by config(listOf(INTENT, STATE, LABEL)) { it.toSet() }

    @Configuration("name of the subpackage of the component package that holds the UI")
    private val uiPackage: String by config("ui")

    @Configuration("nested types of a store that the UI package can also use")
    private val uiTypes: Set<String> by config(listOf(STATE, LABEL)) { it.toSet() }

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)
        StoreReferences(file, storeSuffix, storeTypes)
            .all()
            .distinctBy { it.store to it.type }
            .filterNot { it.isAllowedIn(file.packageFqName) }
            .forEach { reference ->
                val places = listOf("the store package") +
                    allowedSubpackages.map { "`$it`" } +
                    listOfNotNull(
                        "the component package".takeIf { reference.type in componentTypes },
                        "`$uiPackage`".takeIf { reference.type in uiTypes },
                    )
                report(
                    Finding(
                        Entity.from(reference.element),
                        "`${reference.store.shortName()}.${reference.type}` leaks out of the store. " +
                            "Use it only in ${places.joinToString()}.",
                    ),
                )
            }
    }

    private fun StoreReference.isAllowedIn(filePackage: FqName): Boolean {
        val storePackage = store.parent()
        val componentPackages = setOfNotNull(storePackage, storePackage.takeUnless { it.isRoot }?.parent())
        val isInStore = filePackage == storePackage ||
            allowedSubpackages.any { filePackage.isSubpackageOf(storePackage.child(Name.identifier(it))) }
        val isInComponent = type in componentTypes && filePackage in componentPackages
        val isInUi = type in uiTypes &&
            componentPackages.any { filePackage.isSubpackageOf(it.child(Name.identifier(uiPackage))) }
        return isInStore || isInComponent || isInUi
    }

    private companion object {
        const val INTENT = "Intent"
        const val STATE = "State"
        const val LABEL = "Label"
    }
}
