package by.overpas.detekt.architecture

import by.overpas.detekt.core.calleeName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

internal class StoreReferences(
    private val file: KtFile,
    private val suffix: String,
    private val types: Set<String>,
) {

    fun all(): List<StoreReference> {
        val imported = file.importDirectives.mapNotNull { directive ->
            val segments = directive.importedFqName?.pathSegments().orEmpty().map { it.asString() }
            segments.indices
                .firstOrNull { segments[it].endsWith(suffix) && segments.getOrNull(it + 1) in types }
                ?.let { StoreReference(directive, FqName.fromSegments(segments.take(it + 1)), segments[it + 1]) }
        }
        val qualified = file
            .collectDescendantsOfType<KtElement>(
                canGoInside = { it !is KtImportList && it !is KtPackageDirective },
                predicate = { it is KtUserType || it is KtDotQualifiedExpression },
            )
            .mapNotNull { element ->
                val (qualifier, name) = when (element) {
                    is KtUserType -> element.qualifier?.takeIf { it.qualifier == null }?.referencedName to
                        element.referencedName

                    is KtDotQualifiedExpression -> element.selectorExpression.let { selector ->
                        (element.receiverExpression as? KtNameReferenceExpression)?.getReferencedName() to
                            (
                                (selector as? KtNameReferenceExpression)?.getReferencedName()
                                    ?: (selector as? KtCallExpression)?.calleeName()
                                )
                    }

                    else -> null to null
                }
                if (qualifier?.endsWith(suffix) == true && name != null && name in types) {
                    StoreReference(element, storeNamed(qualifier), name)
                } else {
                    null
                }
            }
        return imported + qualified
    }

    private fun storeNamed(name: String): FqName =
        file.importDirectives
            .firstOrNull { !it.isAllUnder && it.importedName?.asString() == name }
            ?.importedFqName
            ?: file.packageFqName.child(Name.identifier(name))
}
