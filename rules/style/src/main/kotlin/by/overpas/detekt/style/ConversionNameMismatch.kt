package by.overpas.detekt.style

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
            "`toMessage(): SyncAlert` promises a message but gives an alert.",
    ) {

    @Configuration("suffixes of a conversion name that the return type does not have to contain")
    private val ignoredSuffixes: List<String> by config(listOf("OrNull", "OrThrow", "OrDefault", "OrElse", "OrEmpty"))

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        val target = function.name?.let(CONVERSION::matchEntire)?.run { groupValues[1] } ?: return
        val subject = ignoredSuffixes.fold(target) { name, suffix -> name.removeSuffix(suffix) }
        val element = function.typeReference?.typeElement
        val type = (if (element is KtNullableType) element.innerType else element) as? KtUserType ?: return
        val typeName = generateSequence(type) { it.qualifier }
            .mapNotNull { it.referencedName }
            .toList()
            .asReversed()
            .joinToString("")
        val typeParameters = function.typeParameters.mapNotNull { it.name }
        if (typeName !in typeParameters && !typeName.contains(subject, ignoreCase = true)) {
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

    private companion object {
        val CONVERSION = Regex("(?:to|as)([A-Z]\\w*)")
    }
}
