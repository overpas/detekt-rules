package by.overpas.detekt.style

import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isSubpackageOf
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtPrimaryConstructor

class CollaboratorNameMismatch(config: Config) :
    Rule(
        config,
        "The name of a constructor property must contain the last word of the name of its type. " +
            "`gateway: PaymentGateway` tells what the property is, `payments: PaymentGateway` does not.",
    ),
    RequiresAnalysisApi {

    @Configuration("packages of the types that a property can have under any name, with subpackages")
    private val excludedPackages: List<FqName> by config(
        listOf("kotlin", "kotlinx", "java", "javax", "dagger"),
    ) { names ->
        names.map(::FqName)
    }

    @Configuration("ignore the constructors of data classes")
    private val areDataClassesIgnored: Boolean by config(true)

    override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor) {
        super.visitPrimaryConstructor(constructor)
        val owner = constructor.getContainingClassOrObject()
        if (areDataClassesIgnored && owner is KtClass && owner.isData()) return
        val delegates = owner.superTypeListEntries
            .filterIsInstance<KtDelegatedSuperTypeEntry>()
            .mapNotNull { (it.delegateExpression as? KtNameReferenceExpression)?.getReferencedName() }
            .toSet()
        constructor.valueParameters
            .filter { parameter ->
                val element = parameter.typeReference?.typeElement
                val named = if (element is KtNullableType) element.innerType else element
                parameter.hasValOrVar() &&
                    parameter.name !in delegates &&
                    named != null &&
                    named !is KtFunctionType
            }
            .mapNotNull { parameter ->
                analyze(parameter) {
                    val type = parameter.typeReference?.type as? KaClassType
                    type?.classId
                        ?.takeUnless { classId -> excludedPackages.any(classId.packageFqName::isSubpackageOf) }
                        ?.run { shortClassName.asString() }
                }?.let { typeName ->
                    WORD_PATTERN.findAll(typeName).lastOrNull()?.let { Triple(parameter, typeName, it.value) }
                }
            }
            .forEach { (parameter, typeName, lastWord) ->
                val name = parameter.nameAsSafeName.asString()
                if (!name.contains(lastWord, ignoreCase = true)) {
                    report(
                        Finding(
                            Entity.from(parameter),
                            "`$name` does not name its type `$typeName`. Put `$lastWord` in the name, " +
                                "for example `${typeName.replaceFirstChar(Char::lowercaseChar)}`.",
                        ),
                    )
                }
            }
    }

    private companion object {
        val WORD_PATTERN = Regex("""\p{Lu}+(?!\p{Ll})|\p{Lu}?[\p{Ll}\d]+""")
    }
}
