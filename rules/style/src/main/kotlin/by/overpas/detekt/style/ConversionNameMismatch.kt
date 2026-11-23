package by.overpas.detekt.style

import by.overpas.detekt.core.shortTypeName
import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtUserType

class ConversionNameMismatch(config: Config) :
    Rule(
        config,
        "The return type of a `toX` or an `asX` function must contain `X` in its name. " +
            "`toInvoice(): Receipt` promises an invoice but gives a receipt.",
    ) {

    @Configuration("suffixes of a conversion name that the return type does not have to contain")
    private val ignoredSuffixes: List<String> by config(listOf("OrNull", "OrThrow", "OrDefault", "OrElse", "OrEmpty"))

    @Configuration("short names of the receiver types whose conversions the rule does not check")
    private val ignoredReceiverTypes: Set<String> by config(
        listOf("String", "Boolean", "Byte", "Short", "Int", "Long", "Float", "Double", "Char"),
    ) { it.toSet() }

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        val target = function.name
            ?.takeUnless { function.receiverTypeReference?.shortTypeName() in ignoredReceiverTypes }
            ?.let(CONVERSION::matchEntire)
            ?.run { groupValues[1] }
            ?: return
        val subject = ignoredSuffixes.fold(target) { name, suffix -> name.removeSuffix(suffix) }
        val element = function.typeReference?.typeElement
        val type = (if (element is KtNullableType) element.innerType else element) as? KtUserType ?: return
        val typeName = type.text.filter(Char::isLetterOrDigit)
        val typeParameters = function.typeParameters.mapNotNull { it.name }
        if (typeName !in typeParameters && !typeName.names(subject)) {
            report(
                Finding(
                    Entity.atName(function),
                    "`${function.nameAsSafeName}` returns `${function.typeReference?.text.orEmpty()}`, " +
                        "which is not `$subject`. " +
                        "Name the function after its return type, or return `$subject`.",
                ),
            )
        }
    }

    private fun String.names(subject: String): Boolean {
        val singulars = PLURAL_ENDINGS
            .map { (ending, replacement) -> subject.replace(ending, replacement) }
            .filter { it.isNotEmpty() && it != subject }
        return (singulars + subject).any { contains(it, ignoreCase = true) }
    }

    private companion object {
        val CONVERSION = Regex("(?:to|as)([A-Z]\\w*)")
        val PLURAL_ENDINGS = listOf(Regex("ies$") to "y", Regex("es$") to "", Regex("s$") to "")
    }
}
