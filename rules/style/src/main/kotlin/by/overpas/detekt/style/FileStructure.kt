package by.overpas.detekt.style

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration

private const val NESTED_CLASS = "nested class or object"

private val TOP_LEVEL_SLOTS = listOf(
    "constant",
    "property",
    "interface",
    "class or object",
    "function or extension property",
)

private val CLASS_SLOTS = listOf(
    "enum entry",
    "private property",
    "non-private property",
    "initializer or secondary constructor",
    "function",
    "companion object",
    "nested interface",
    NESTED_CLASS,
)

private val INTERFACE_SLOTS = listOf(
    "property",
    "function",
    "companion object",
    "nested interface",
    NESTED_CLASS,
)

class FileStructure(config: Config) :
    Rule(
        config,
        "A file must follow the project layout: constants, properties, one interface, one class and functions. " +
            "Many classes are allowed only when all of them extend the file interface. " +
            "Class and interface bodies must follow the member order too.",
    ) {

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)
        val declarations = file.declarations
        checkOrder(declarations, TOP_LEVEL_SLOTS, ::topLevelSlot)
        reportExtra(
            declarations.filter { it.isInterfaceDeclaration() },
            "A file can have at most one top-level interface.",
        )
        val interfaceName = declarations.firstOrNull { it.isInterfaceDeclaration() }?.name
        val classes = declarations.filterIsInstance<KtClassOrObject>().filter { it.isCountedClass() }
        if (interfaceName == null || !classes.all { it.extends(interfaceName) }) {
            reportExtra(
                classes,
                "A file can have at most one top-level class or object, unless all of them extend the file interface.",
            )
        }
    }

    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
        super.visitClassOrObject(classOrObject)
        if (classOrObject.isInterfaceDeclaration()) {
            checkOrder(classOrObject.declarations, INTERFACE_SLOTS, ::interfaceSlot)
        } else {
            checkOrder(classOrObject.declarations, CLASS_SLOTS, ::classSlot)
        }
    }

    private fun checkOrder(
        declarations: List<KtDeclaration>,
        labels: List<String>,
        slotOf: (KtDeclaration) -> Int?,
    ) {
        declarations
            .mapNotNull { declaration -> slotOf(declaration)?.let { declaration to it } }
            .fold(null as Pair<KtDeclaration, Int>?) { furthest, current ->
                if (furthest != null && current.second < furthest.second) {
                    report(
                        Finding(
                            Entity.from(current.first),
                            "Move `${current.first.displayName()}` (${labels[current.second]}) before " +
                                "`${furthest.first.displayName()}` (${labels[furthest.second]}).",
                        ),
                    )
                    furthest
                } else {
                    current
                }
            }
    }

    private fun reportExtra(
        declarations: List<KtDeclaration>,
        reason: String,
    ) {
        declarations.drop(1).forEach { declaration ->
            report(
                Finding(
                    Entity.from(declaration),
                    "Move `${declaration.displayName()}` to its own file. $reason",
                ),
            )
        }
    }

    private fun topLevelSlot(declaration: KtDeclaration): Int? =
        when {
            declaration is KtProperty && declaration.hasModifier(KtTokens.CONST_KEYWORD) -> 0
            declaration is KtProperty && declaration.isExtensionDeclaration() -> 4
            declaration is KtProperty || declaration is KtTypeAlias -> 1
            declaration.isInterfaceDeclaration() -> 2
            declaration is KtClassOrObject -> 3
            declaration is KtNamedFunction -> 4
            else -> null
        }

    private fun classSlot(declaration: KtDeclaration): Int? =
        when {
            declaration is KtEnumEntry -> 0
            declaration is KtProperty && declaration.hasModifier(KtTokens.PRIVATE_KEYWORD) -> 1
            declaration is KtProperty -> 2
            declaration is KtAnonymousInitializer || declaration is KtSecondaryConstructor -> 3
            declaration is KtNamedFunction -> 4
            declaration is KtObjectDeclaration && declaration.isCompanion() -> 5
            declaration.isInterfaceDeclaration() -> 6
            declaration is KtClassOrObject -> 7
            else -> null
        }

    private fun interfaceSlot(declaration: KtDeclaration): Int? =
        when {
            declaration is KtProperty -> 0
            declaration is KtNamedFunction -> 1
            declaration is KtObjectDeclaration && declaration.isCompanion() -> 2
            declaration.isInterfaceDeclaration() -> 3
            declaration is KtClassOrObject -> 4
            else -> null
        }

    private fun KtDeclaration.isInterfaceDeclaration(): Boolean =
        this is KtClass && isInterface()

    private fun KtDeclaration.isCountedClass(): Boolean =
        this is KtClassOrObject && !isInterfaceDeclaration() && !(this is KtClass && isAnnotation())

    private fun KtClassOrObject.extends(name: String): Boolean =
        superTypeListEntries.any { (it.typeReference?.typeElement as? KtUserType)?.referencedName == name }

    private fun KtDeclaration.displayName(): String =
        when (this) {
            is KtAnonymousInitializer -> "init"
            is KtSecondaryConstructor -> "constructor"
            is KtObjectDeclaration if isCompanion() -> name ?: "Companion"
            is KtNamedDeclaration -> name ?: "<anonymous>"
            else -> text.lineSequence().first()
        }
}
