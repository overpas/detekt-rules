package by.overpas.detekt.architecture

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
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaLocalVariableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isSubpackageOf
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

class ConstructedCollaborator(config: Config) :
    Rule(
        config,
        "A class must not build a collaborator from its constructor parameters " +
            "in a property initializer or in an init block. Inject the collaborator instead.",
    ),
    RequiresAnalysisApi {

    @Configuration("packages of the classes that a class can build from its constructor parameters, with subpackages")
    private val excludedPackages: List<FqName> by config(
        listOf("kotlin", "kotlinx", "java", "javax", "android", "androidx"),
    ) { names ->
        names.map(::FqName)
    }

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)
        val parameters = klass.primaryConstructorParameters.toSet()
        if (parameters.isEmpty()) return
        val properties = klass.body?.properties.orEmpty().map { property ->
            Triple<KtElement, String, List<KtExpression>>(
                property,
                "`${property.nameAsSafeName}`",
                listOfNotNull(property.initializer, property.delegateExpression),
            )
        }
        val initializers = klass.getAnonymousInitializers().map { initializer ->
            Triple<KtElement, String, List<KtExpression>>(
                initializer,
                "The init block",
                listOfNotNull(initializer.body),
            )
        }
        (properties + initializers).forEach { (element, subject, expressions) ->
            val built = analyze(klass) { BuiltClasses(this, parameters, excludedPackages).nameIn(expressions) }
            if (built != null) {
                report(
                    Finding(
                        Entity.from(element),
                        "$subject builds `$built` from constructor parameters. " +
                            "Inject `$built` or its interface instead.",
                    ),
                )
            }
        }
    }

    private class BuiltClasses(
        private val session: KaSession,
        private val parameters: Set<KtParameter>,
        private val excludedPackages: List<FqName>,
    ) {

        fun nameIn(expressions: List<KtExpression>): String? =
            with(session) {
                expressions
                    .asSequence()
                    .flatMap { it.collectDescendantsOfType<KtCallExpression>() }
                    .filter(::isBuiltFromParameters)
                    .mapNotNull { call ->
                        val callee = call.resolveToCall()?.singleFunctionCallOrNull()?.symbol
                        (callee as? KaConstructorSymbol)?.run { returnType.expandedSymbol } as? KaNamedClassSymbol
                    }
                    .firstOrNull { symbol ->
                        val packageName = symbol.classId?.packageFqName
                        !symbol.isData &&
                            !symbol.isInline &&
                            packageName != null &&
                            excludedPackages.none(packageName::isSubpackageOf)
                    }
                    ?.run { name.asString() }
            }

        private fun isBuiltFromParameters(call: KtCallExpression): Boolean =
            with(session) {
                val (parameterVariables, otherVariables) = call.valueArguments
                    .asSequence()
                    .mapNotNull { it.getArgumentExpression() }
                    .flatMap { it.collectDescendantsOfType<KtNameReferenceExpression>() }
                    .mapNotNull { it.mainReference.resolveToSymbol() as? KaVariableSymbol }
                    .partition { it.psi in parameters }
                parameterVariables.isNotEmpty() &&
                    otherVariables.none { it is KaLocalVariableSymbol || it is KaValueParameterSymbol }
            }
    }
}
