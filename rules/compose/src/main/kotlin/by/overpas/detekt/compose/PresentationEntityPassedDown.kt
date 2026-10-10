package by.overpas.detekt.compose

import by.overpas.detekt.core.hasAnnotation
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
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class PresentationEntityPassedDown(config: Config) :
    Rule(
        config,
        "A presentation entity, such as a component, a view model, a presenter or a store, " +
            "must stay in the entrypoint composable of a screen. " +
            "Give state values and callbacks to the other composables.",
    ),
    RequiresAnalysisApi {

    @Configuration("short names of the annotations that mark a composable function")
    private val composableAnnotations: Set<String> by config(listOf("Composable")) { it.toSet() }

    @Configuration("suffixes of the names of the presentation entity types, also checked on their supertypes")
    private val entitySuffixes: List<String> by config(
        listOf("Component", "ViewModel", "Presenter", "StateHolder", "Store", "ScreenModel"),
    )

    @Configuration("fully qualified names of the presentation entity types, with their subtypes")
    private val entityTypes: Set<ClassId> by config(
        listOf(
            "androidx.lifecycle.ViewModel",
            "com.arkivanov.decompose.ComponentContext",
            "com.arkivanov.mvikotlin.core.store.Store",
            "cafe.adriel.voyager.core.model.ScreenModel",
        ),
    ) { names -> names.map { ClassId.topLevel(FqName(it)) }.toSet() }

    @Configuration("fully qualified names of the types that are not presentation entities, with their subtypes")
    private val excludedTypes: Set<ClassId> by config(
        listOf(
            "java.awt.Component",
            "androidx.compose.runtime.saveable.SaveableStateHolder",
            "androidx.lifecycle.ViewModelStore",
            "androidx.datastore.core.DataStore",
        ),
    ) { names -> names.map { ClassId.topLevel(FqName(it)) }.toSet() }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val callee = expression.calleeExpression?.text ?: return
        val owner = expression.getStrictParentOfType<KtNamedFunction>()
        if (expression.valueArguments.isEmpty() || owner?.hasAnnotation(composableAnnotations) != true) return
        analyze(expression) { entityArguments(expression) }
            .forEach { (argument, typeName) ->
                report(
                    Finding(
                        Entity.from(argument),
                        "`${argument.text}` passes the presentation entity `$typeName` down to `$callee`. " +
                            "Pass state values and callbacks, such as method references, instead.",
                    ),
                )
            }
    }

    private fun KaSession.entityArguments(call: KtCallExpression): List<Pair<KtValueArgument, String>> {
        val callee = call.resolveToCall()?.singleFunctionCallOrNull()?.symbol
        if (callee?.psi?.containingFile != call.containingFile) return emptyList()
        val types = EntityTypes(
            session = this,
            suffixes = entitySuffixes,
            types = entityTypes,
            excludedTypes = excludedTypes,
        )
        return call.valueArguments.mapNotNull { argument ->
            argument.getArgumentExpression()
                ?.expressionType
                ?.let(types::nameOf)
                ?.let { argument to it }
        }
    }

    private class EntityTypes(
        private val session: KaSession,
        private val suffixes: List<String>,
        private val types: Set<ClassId>,
        private val excludedTypes: Set<ClassId>,
    ) {

        fun nameOf(type: KaType): String? =
            with(session) {
                (type.withNullability(false) as? KaClassType)
                    ?.takeUnless { candidate -> excludedTypes.any { candidate.isSubtypeOf(it) } }
                    ?.takeIf { candidate ->
                        (candidate.allSupertypes.filterIsInstance<KaClassType>() + candidate).any {
                            it.classId in types || suffixes.any(it.classId.shortClassName.asString()::endsWith)
                        }
                    }
                    ?.run { classId.shortClassName.asString() }
            }
    }
}
