package by.overpas.detekt.coroutines

import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class TryInCoroutineFunction(config: Config) :
    Rule(
        config,
        "try, catch and finally are not idiomatic error handling in coroutine code. " +
            "Use the coroutine and Kotlin error handling tools, " +
            "such as Flow.catch, onCompletion, CoroutineExceptionHandler, a result type or use.",
    ),
    RequiresAnalysisApi {

    @Configuration("fully qualified names of the coroutine scope types, with their subtypes")
    private val scopeTypes: Set<ClassId> by config(listOf("kotlinx.coroutines.CoroutineScope")) { names ->
        names.map { ClassId.topLevel(FqName(it)) }.toSet()
    }

    override fun visitTryExpression(expression: KtTryExpression) {
        super.visitTryExpression(expression)
        val function = expression.getStrictParentOfType<KtNamedFunction>() ?: return
        if (analyze(function) { CoroutineUsage(this, scopeTypes).isIn(function) }) {
            report(
                Finding(
                    Entity.from(expression),
                    "`${function.nameAsSafeName}` uses coroutines. Replace the try with an idiomatic error handling " +
                        "tool, such as Flow.catch, onCompletion, CoroutineExceptionHandler, a result type or use.",
                ),
            )
        }
    }

    private class CoroutineUsage(
        private val session: KaSession,
        private val scopeTypes: Set<ClassId>,
    ) {

        fun isIn(function: KtNamedFunction): Boolean =
            with(session) {
                usesCoroutines(function.symbol) ||
                    function
                        .collectDescendantsOfType<KtCallExpression> {
                            it.getStrictParentOfType<KtNamedFunction>() == function
                        }
                        .mapNotNull { it.resolveToCall()?.singleFunctionCallOrNull() }
                        .any { usesCoroutines(it.symbol) }
            }

        private fun usesCoroutines(symbol: KaFunctionSymbol): Boolean =
            with(session) {
                val types = symbol.valueParameters.map { it.returnType } +
                    listOfNotNull(symbol.receiverParameter?.returnType)
                (symbol as? KaNamedFunctionSymbol)?.isSuspend == true ||
                    types.any { type ->
                        type.isSuspendFunctionType || scopeTypes.any { type.isSubtypeOf(it) }
                    }
            }
    }
}
