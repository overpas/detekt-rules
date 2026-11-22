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
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtSecondaryConstructor

class ConcreteCollaborator(config: Config) :
    Rule(
        config,
        "A constructor must take a collaborator through its interface. " +
            "A class that implements a project interface ties the constructor to one implementation.",
    ),
    RequiresAnalysisApi {

    @Configuration("number of the first package segments that a project interface shares with the checked file")
    private val rootPackageDepth: Int by config(2)

    @Configuration("ignore the constructors of data classes")
    private val areDataClassesIgnored: Boolean by config(true)

    override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor) {
        super.visitPrimaryConstructor(constructor)
        check(constructor)
    }

    override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor) {
        super.visitSecondaryConstructor(constructor)
        check(constructor)
    }

    private fun check(constructor: KtConstructor<*>) {
        val owner = constructor.getContainingClassOrObject()
        if (areDataClassesIgnored && owner is KtClass && owner.isData()) return
        val projectClasses = ProjectClasses(constructor.containingKtFile, rootPackageDepth)
        constructor.valueParameters.forEach { parameter ->
            val interfaces = analyze(parameter) { projectInterfaces(parameter, projectClasses) }
            if (interfaces.isNotEmpty()) {
                report(
                    Finding(
                        Entity.from(parameter),
                        "`${parameter.nameAsSafeName}` has the concrete type " +
                            "`${parameter.typeReference?.text.orEmpty()}`. " +
                            "Use its interface instead: ${interfaces.joinToString { "`$it`" }}.",
                    ),
                )
            }
        }
    }

    private fun KaSession.projectInterfaces(
        parameter: KtParameter,
        projectClasses: ProjectClasses,
    ): List<String> {
        val type = parameter.typeReference?.run { type.withNullability(false) } as? KaClassType
        val symbol = (type?.expandedSymbol as? KaNamedClassSymbol)
            ?.takeIf { it.classKind == KaClassKind.CLASS && !it.isData && !it.isInline }
        if (type == null || symbol == null) return emptyList()
        return type.allSupertypes
            .mapNotNull { it.expandedSymbol }
            .filter { it.classKind == KaClassKind.INTERFACE && it in projectClasses }
            .mapNotNull { symbol -> symbol.classId?.run { shortClassName.asString() } }
            .toList()
    }
}
