package by.overpas.detekt.architecture

import com.intellij.psi.PsiElement
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.psi.psiUtil.isPrivate

class HiddenAbstraction(config: Config) :
    Rule(
        config,
        "A class, an interface, an object or a file must not have more helper functions than entry points. " +
            "Many helper functions that call each other show a hidden abstraction. " +
            "Put the helpers behind a new abstraction, or move them to the types that they use.",
    ),
    RequiresAnalysisApi {

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)
        check(Scope(file), Entity.atPackageOrFirstDecl(file), "The top level of the file")
    }

    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
        super.visitClassOrObject(classOrObject)
        if (classOrObject is KtObjectDeclaration && classOrObject.isCompanion()) return
        val subject = classOrObject.name?.let { "`$it`" } ?: "The object expression"
        check(Scope(classOrObject), Entity.atName(classOrObject), subject)
    }

    private fun check(
        scope: Scope,
        entity: Entity,
        subject: String,
    ) {
        val helpers = scope.helpers().size
        val entryPoints = scope.entryPoints().size
        if (helpers <= entryPoints) return
        report(
            Finding(
                entity,
                "$subject has more helper functions ($helpers) than entry points ($entryPoints). " +
                    "Put the helpers behind a new abstraction, or move them to the types that they use.",
            ),
        )
    }

    private class Scope(private val element: KtElement) {

        private val functions = element.collectDescendantsOfType<KtNamedFunction>(
            canGoInside = { child ->
                when (child) {
                    element -> true
                    is KtObjectDeclaration -> child.isCompanion()
                    else -> child !is KtClassOrObject
                }
            },
        )

        private val names = functions.map { it.nameAsSafeName }.toSet()

        private val callees: Set<PsiElement> by lazy {
            analyze(element) {
                element.collectDescendantsOfType<KtReferenceExpression>()
                    .asSequence()
                    .flatMap { expression -> expression.references.filterIsInstance<KtReference>() }
                    .filter { reference -> reference.resolvesByNames.any { it in names } }
                    .flatMap { reference ->
                        reference.resolveToSymbols()
                            .mapNotNull { it.psi }
                            .filterNot { it.isAncestor(reference.element) }
                    }
                    .toSet()
            }
        }

        fun helpers(): List<KtNamedFunction> =
            functions.filter { !it.isContract() && it in callees }

        fun entryPoints(): List<KtNamedFunction> =
            functions.filter { it.isContract() } +
                functions.filterNot { it.isContract() || it in callees || it.isPrivate() || it.isLocal }

        private fun KtNamedFunction.isContract(): Boolean =
            hasModifier(KtTokens.OVERRIDE_KEYWORD) || !hasBody()
    }
}
