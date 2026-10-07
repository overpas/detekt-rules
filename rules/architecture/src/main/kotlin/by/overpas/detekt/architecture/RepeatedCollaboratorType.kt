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
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtSecondaryConstructor

class RepeatedCollaboratorType(config: Config) :
    Rule(
        config,
        "A constructor must not take more than one collaborator of the same type. " +
            "A group of same-type collaborators tends to grow, so the class does not scale. " +
            "Put the group behind one abstraction.",
    ),
    RequiresAnalysisApi {

    @Configuration("fully qualified names of the types that a constructor can take more than once, with their subtypes")
    private val excludedTypes: Set<ClassId> by config(
        listOf(
            "kotlin.Boolean",
            "kotlin.Char",
            "kotlin.Number",
            "kotlin.UByte",
            "kotlin.UShort",
            "kotlin.UInt",
            "kotlin.ULong",
            "kotlin.CharSequence",
            "kotlin.collections.Iterable",
            "kotlin.collections.Map",
            "kotlin.Array",
            "kotlin.BooleanArray",
            "kotlin.CharArray",
            "kotlin.ByteArray",
            "kotlin.ShortArray",
            "kotlin.IntArray",
            "kotlin.LongArray",
            "kotlin.FloatArray",
            "kotlin.DoubleArray",
            "kotlin.sequences.Sequence",
            "kotlin.Function",
            "kotlin.Enum",
            "kotlin.time.Duration",
            "kotlin.time.Instant",
            "kotlin.uuid.Uuid",
            "kotlin.coroutines.CoroutineContext",
            "kotlinx.coroutines.CoroutineDispatcher",
        ),
    ) { names -> names.map { ClassId.topLevel(FqName(it)) }.toSet() }

    @Configuration("fully qualified names of the supertypes that do not make two type arguments related")
    private val ignoredSupertypes: Set<ClassId> by config(
        listOf("kotlin.Any", "kotlin.Comparable", "java.io.Serializable"),
    ) { names -> names.map { ClassId.topLevel(FqName(it)) }.toSet() }

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
        analyze(constructor) { repeatedCollaborators(constructor.valueParameters) }
            .forEach { (typeName, parameters) ->
                report(
                    Finding(
                        Entity.from(parameters.first()),
                        parameters.joinToString { "`${it.nameAsSafeName.asString()}`" } +
                            " have the same type `$typeName`. Put the collaborators behind one abstraction.",
                    ),
                )
            }
    }

    private fun KaSession.repeatedCollaborators(parameters: List<KtParameter>): Map<String, List<KtParameter>> {
        val types = CollaboratorTypes(this, excludedTypes, ignoredSupertypes)
        val collaborators = parameters.mapNotNull { parameter ->
            types.typeOf(parameter)?.let { parameter to it }
        }
        return collaborators
            .groupBy(
                keySelector = { (_, type) ->
                    collaborators.first { it.second.classId == type.classId && types.isRelated(it.second, type) }
                },
                valueTransform = { (parameter, _) -> parameter },
            )
            .filterValues { it.size > 1 }
            .mapKeys { (collaborator, _) -> collaborator.second.classId.shortClassName.asString() }
    }

    private class CollaboratorTypes(
        private val session: KaSession,
        private val excludedTypes: Set<ClassId>,
        private val ignoredSupertypes: Set<ClassId>,
    ) {

        fun typeOf(parameter: KtParameter): KaClassType? =
            with(session) {
                val type = parameter.typeReference?.run { type.withNullability(false) } as? KaClassType
                type?.takeUnless { candidate -> excludedTypes.any { candidate.isSubtypeOf(it) } }
            }

        fun isRelated(
            first: KaType?,
            second: KaType?,
        ): Boolean =
            with(session) {
                val firstClass = first?.withNullability(false) as? KaClassType
                val secondClass = second?.withNullability(false) as? KaClassType
                when {
                    firstClass == null || secondClass == null -> true

                    firstClass.classId == secondClass.classId ->
                        firstClass.typeArguments
                            .zip(secondClass.typeArguments)
                            .all { (firstArgument, secondArgument) ->
                                isRelated(firstArgument.type, secondArgument.type)
                            }

                    firstClass.isSubtypeOf(secondClass) || secondClass.isSubtypeOf(firstClass) -> true

                    else -> supertypeIds(firstClass).intersect(supertypeIds(secondClass)).isNotEmpty()
                }
            }

        private fun supertypeIds(type: KaClassType): Set<ClassId> =
            with(session) {
                type.allSupertypes
                    .filterIsInstance<KaClassType>()
                    .map { it.classId }
                    .filterNot { it in ignoredSupertypes }
                    .toSet()
            }
    }
}
