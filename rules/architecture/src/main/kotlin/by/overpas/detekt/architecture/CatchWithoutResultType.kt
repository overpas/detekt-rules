package by.overpas.detekt.architecture

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType

class CatchWithoutResultType(config: Config) :
    Rule(
        config,
        "A function with a try and a catch must return kotlin.Result or a custom result type, " +
            "such as an enum, a sealed interface or a sealed class. An unexpected exception must fail fast.",
    ),
    RequiresAnalysisApi {

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        val hasCatch = function.bodyExpression?.anyDescendantOfType<KtTryExpression>(
            canGoInside = { it !is KtNamedFunction || it.name == null },
            predicate = { it.catchClauses.isNotEmpty() },
        ) == true
        if (function.name != null && hasCatch && !analyze(function) { isResultType(function.symbol.returnType) }) {
            report(
                Finding(
                    Entity.atName(function),
                    "`${function.nameAsSafeName}` catches an exception but does not return a result type. " +
                        "Return kotlin.Result or a custom result type, such as an enum, a sealed interface " +
                        "or a sealed class, or let the exception fail fast.",
                ),
            )
        }
    }

    private fun KaSession.isResultType(type: KaType): Boolean {
        val symbol = type.expandedSymbol
        return when {
            type is KaErrorType -> true

            symbol == null || type.fullyExpandedType.isMarkedNullable -> false

            else ->
                symbol.classId == StandardClassIds.Result ||
                    symbol.classKind == KaClassKind.ENUM_CLASS ||
                    symbol.modality == KaSymbolModality.SEALED
        }
    }
}
