package by.overpas.mauth.detekt

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
        val collaborators = parameters.mapNotNull { parameter ->
            collaboratorType(parameter)?.let { parameter to it }
        }
        return collaborators
            .groupBy(
                keySelector = { (_, type) -> collaborators.first { isSameType(it.second, type) } },
                valueTransform = { (parameter, _) -> parameter },
            )
            .filterValues { it.size > 1 }
            .mapKeys { (collaborator, _) -> collaborator.second.classId.shortClassName.asString() }
    }

    private fun KaSession.collaboratorType(parameter: KtParameter): KaClassType? {
        val type = parameter.typeReference?.run { type.withNullability(false) } as? KaClassType
        return type?.takeUnless { candidate -> excludedTypes.any { candidate.isSubtypeOf(it) } }
    }

    private fun KaSession.isSameType(
        first: KaClassType,
        second: KaClassType,
    ): Boolean =
        first.classId == second.classId &&
            first.typeArguments.zip(second.typeArguments).all { (firstArgument, secondArgument) ->
                isRelated(firstArgument.type, secondArgument.type)
            }

    private fun KaSession.isRelated(
        first: KaType?,
        second: KaType?,
    ): Boolean {
        val firstClass = first?.withNullability(false) as? KaClassType
        val secondClass = second?.withNullability(false) as? KaClassType
        return when {
            firstClass == null || secondClass == null -> true
            firstClass.classId == secondClass.classId -> isSameType(firstClass, secondClass)
            firstClass.isSubtypeOf(secondClass) || secondClass.isSubtypeOf(firstClass) -> true
            else -> supertypeIds(firstClass).intersect(supertypeIds(secondClass)).isNotEmpty()
        }
    }

    private fun KaSession.supertypeIds(type: KaClassType): Set<ClassId> =
        type.allSupertypes
            .filterIsInstance<KaClassType>()
            .map { it.classId }
            .filterNot { it in ignoredSupertypes }
            .toSet()
}
