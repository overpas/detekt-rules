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
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

class LowCohesion(config: Config) :
    Rule(
        config,
        "A class must not contain unrelated groups of members. " +
            "Each group of properties and the functions that use them is a separate responsibility. " +
            "Split the class along the groups.",
    ),
    RequiresAnalysisApi {

    @Configuration("maximum number of unrelated groups of members in a class")
    private val allowedComponents: Int by config(1)

    @Configuration("minimum number of functions in the groups before the rule checks a class")
    private val minFunctions: Int by config(3)

    @Configuration("names of the functions that do not link the members of a class")
    private val ignoredFunctions: Set<String> by config(listOf("equals", "hashCode", "toString")) { it.toSet() }

    @Configuration("ignore data classes")
    private val areDataClassesIgnored: Boolean by config(true)

    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
        super.visitClassOrObject(classOrObject)
        if (!isChecked(classOrObject)) return
        val groups = analyze(classOrObject) { MemberGraph(this, classOrObject, ignoredFunctions).groups() }
        val functions = groups.sumOf { group -> group.count { it is KtNamedFunction } }
        if (groups.size <= allowedComponents || functions < minFunctions) return
        val listedGroups = groups.joinToString { group ->
            group.joinToString(prefix = "{", postfix = "}") { member ->
                val name = member.nameAsSafeName.asString()
                if (member is KtNamedFunction) "`$name()`" else "`$name`"
            }
        }
        val subject = classOrObject.nameAsSafeName.asString()
        report(
            Finding(
                Entity.atName(classOrObject),
                "`$subject` has ${groups.size} unrelated groups of members: $listedGroups. " +
                    "Split the class along the groups.",
            ),
        )
    }

    private fun isChecked(classOrObject: KtClassOrObject): Boolean =
        classOrObject.superTypeListEntries.none { it is KtDelegatedSuperTypeEntry } &&
            when (classOrObject) {
                is KtEnumEntry -> false

                is KtClass ->
                    !classOrObject.isInterface() &&
                        !classOrObject.isEnum() &&
                        !classOrObject.isAnnotation() &&
                        !(areDataClassesIgnored && classOrObject.isData())

                is KtObjectDeclaration -> !classOrObject.isObjectLiteral()

                else -> false
            }

    private class MemberGraph(
        private val session: KaSession,
        classOrObject: KtClassOrObject,
        ignoredFunctions: Set<String>,
    ) {

        private val members: Set<KtNamedDeclaration> =
            (
                classOrObject.primaryConstructorParameters.filter { it.hasValOrVar() } +
                    classOrObject.declarations.filterIsInstance<KtProperty>() +
                    classOrObject.declarations
                        .filterIsInstance<KtNamedFunction>()
                        .filterNot { it.name in ignoredFunctions }
                ).toSet()

        private val names = members.mapNotNull { it.name }.toSet()

        fun groups(): List<List<KtNamedDeclaration>> =
            members
                .map { member -> linkedMembers(member) + member }
                .fold(emptyList<Set<KtNamedDeclaration>>()) { groups, links ->
                    val (joined, separate) = groups.partition { group -> group.any { it in links } }
                    separate.plusElement(joined.flatten().toSet() + links)
                }
                .asSequence()
                .filter { it.size > 1 }
                .map { group -> group.sortedBy { it.textOffset } }
                .sortedBy { it.first().textOffset }
                .toList()

        private fun linkedMembers(member: KtNamedDeclaration): Set<KtNamedDeclaration> {
            val bodies: List<KtElement> =
                when (member) {
                    is KtNamedFunction -> listOf(member)
                    is KtProperty -> listOfNotNull(member.getter, member.setter, member.delegateExpression)
                    else -> emptyList()
                }
            return with(session) {
                bodies
                    .asSequence()
                    .flatMap { it.collectDescendantsOfType<KtNameReferenceExpression>() }
                    .filter { it.getReferencedName() in names }
                    .flatMap { expression -> expression.references.filterIsInstance<KtReference>() }
                    .flatMap { reference -> reference.resolveToSymbols().mapNotNull { it.psi } }
                    .filterIsInstance<KtNamedDeclaration>()
                    .filter { it in members }
                    .toSet()
            }
        }
    }
}
