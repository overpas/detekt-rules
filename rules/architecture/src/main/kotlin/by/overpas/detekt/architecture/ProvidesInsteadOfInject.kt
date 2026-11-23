package by.overpas.detekt.architecture

import by.overpas.detekt.core.hasAnnotation
import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtReturnExpression

class ProvidesInsteadOfInject(config: Config) :
    Rule(
        config,
        "A DI provider must not only pass its parameters to the constructor of a project class. " +
            "Put `@Inject` on the class, and bind it to its interface with `@Binds`.",
    ),
    RequiresAnalysisApi {

    @Configuration("short names of the annotations that mark a DI provider")
    private val providerAnnotations: Set<String> by config(listOf("Provides")) { it.toSet() }

    @Configuration("number of the first package segments that a project class shares with the checked file")
    private val rootPackageDepth: Int by config(2)

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        val call = function.takeIf { it.hasAnnotation(providerAnnotations) }?.forwardingCall() ?: return
        val projectClasses = ProjectClasses(function.containingKtFile, rootPackageDepth)
        val (built, provided) = analyze(function) {
            val constructor = call.resolveToCall()?.singleFunctionCallOrNull()?.symbol as? KaConstructorSymbol
            val symbol = constructor?.run { returnType.expandedSymbol }?.takeIf { it in projectClasses }
            symbol?.classId to (function.symbol.returnType as? KaClassType)?.classId
        }
        if (built != null) {
            val className = built.shortClassName.asString()
            val fix = if (provided == null || provided == built) {
                "Put `@Inject` on `$className` and remove the provider."
            } else {
                "Put `@Inject` on `$className` and bind it to `${provided.shortClassName}` with `@Binds`."
            }
            report(
                Finding(
                    Entity.atName(function),
                    "`${function.nameAsSafeName}` only passes its parameters to `$className`. $fix",
                ),
            )
        }
    }

    private fun KtNamedFunction.forwardingCall(): KtCallExpression? {
        val body = bodyExpression
        val returned = ((body as? KtBlockExpression)?.run { statements.singleOrNull() } as? KtReturnExpression)
            ?.returnedExpression
        val parameters = valueParameters.map { it.nameAsSafeName.asString() }.toSet()
        return ((returned ?: body) as? KtCallExpression)?.takeIf { call ->
            call.lambdaArguments.isEmpty() &&
                call.valueArguments.all { argument ->
                    (argument.getArgumentExpression() as? KtNameReferenceExpression)?.getReferencedName() in parameters
                }
        }
    }
}
