package by.overpas.detekt.architecture

import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFile

internal interface ScoredParts {
    val name: String
    val parts: List<Part>
}

internal class ClassParts(
    private val classOrObject: KtClassOrObject,
    cost: MemberCost,
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
        if (classOrObject is KtClass && classOrObject.isInner()) Part(name, parts) else Part(name, 1.0, 0.0)
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
