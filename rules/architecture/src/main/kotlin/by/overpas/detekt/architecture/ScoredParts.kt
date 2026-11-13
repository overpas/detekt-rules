package by.overpas.detekt.architecture

import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtObjectDeclaration

private const val LISTED_PARTS = 3

internal interface ScoredParts : Measured {
    val parts: List<Part>

    override fun total(score: (Part) -> Double): Double =
        parts.sumOf(score)

    override fun details(score: (Part) -> Double): String =
        parts
            .asSequence()
            .filter { score(it) > 0 }
            .sortedByDescending(score)
            .take(LISTED_PARTS)
            .joinToString(prefix = "Largest parts: ", postfix = ".") { "`${it.name}` ${score(it).oneDecimal()}" }
}

internal class ClassParts(
    private val classOrObject: KtClassOrObject,
    private val cost: MemberCost,
) : ScoredParts {

    private val nested: List<ClassParts> =
        classOrObject.declarations
            .asSequence()
            .filterIsInstance<KtClassOrObject>()
            .filterNot { it is KtEnumEntry }
            .map { ClassParts(it, cost) }
            .toList()

    private val members: List<Part> =
        classOrObject.primaryConstructorParameters.filter { it.hasValOrVar() }.map(cost::ofParameter) +
            classOrObject.declarations.mapNotNull(cost::ofDeclaration)

    override val name: String = classOrObject.nameAsSafeName.asString()

    override val parts: List<Part> by lazy { members + nested.map { it.asNested() } }

    fun flat(): Part =
        Part(name, members + nested.map { it.flat() })

    private fun asNested(): Part =
        if (classOrObject is KtClass && classOrObject.isInner()) Part(name, parts) else cost.ofNestedClass(name)
}

internal class FileParts(
    file: KtFile,
    cost: MemberCost,
) : ScoredParts {

    override val name: String = file.name

    override val parts: List<Part> =
        file.declarations.mapNotNull { declaration ->
            if (declaration is KtClassOrObject) {
                ClassParts(
                    declaration,
                    cost,
                ).flat()
            } else {
                cost.ofDeclaration(declaration)
            }
        }
}

internal fun KtClassOrObject.isScored(): Boolean =
    this !is KtEnumEntry && !(this is KtObjectDeclaration && isObjectLiteral())
