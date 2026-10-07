package by.overpas.detekt.style

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.parents

internal fun KtDeclaration.isInterfaceDeclaration(): Boolean =
    this is KtClass && isInterface()

internal fun KtDeclaration.isCountedClass(): Boolean =
    this is KtClassOrObject && !isInterfaceDeclaration() && !(this is KtClass && isAnnotation())

internal fun KtClassOrObject.extends(name: String): Boolean =
    superTypeListEntries.any { (it.typeReference?.typeElement as? KtUserType)?.referencedName == name }

internal fun KtDeclaration.displayName(): String =
    when (this) {
        is KtAnonymousInitializer -> "init"
        is KtSecondaryConstructor -> "constructor"
        is KtObjectDeclaration if isCompanion() -> name ?: "Companion"
        is KtNamedDeclaration -> name ?: "<anonymous>"
        else -> text.lineSequence().first()
    }

internal fun KtNameReferenceExpression.declaration(): KtCallableDeclaration? {
    val name = getReferencedName()
    return parents
        .flatMap { it.declarationsInScope() }
        .firstOrNull { it.name == name }
}

private fun PsiElement.declarationsInScope(): Sequence<KtCallableDeclaration> =
    when (this) {
        is KtBlockExpression -> statements.asSequence().filterIsInstance<KtProperty>()

        is KtFunction -> valueParameters.asSequence()

        is KtClassOrObject -> primaryConstructor?.valueParameters.orEmpty().asSequence() +
            declarations.asSequence().filterIsInstance<KtProperty>()

        is KtFile -> declarations.asSequence().filterIsInstance<KtProperty>()

        else -> emptySequence()
    }
