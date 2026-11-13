package by.overpas.detekt.architecture

import by.overpas.detekt.core.hasAnnotation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import kotlin.math.max
import kotlin.math.sqrt

internal class MemberCost(
    private val composableAnnotations: Set<String>,
    private val scoring: Scoring,
) {

    fun ofDeclaration(declaration: KtDeclaration): Part? =
        when (declaration) {
            is KtProperty ->
                Part(
                    declaration.nameAsSafeName.asString(),
                    declaration.propertyScore(),
                    declaration.hasAnnotation(composableAnnotations) ||
                        declaration.getter?.hasAnnotation(composableAnnotations) == true,
                )

            is KtNamedFunction ->
                Part(
                    "${declaration.nameAsSafeName}()",
                    declaration.functionScore(declaration.valueParameters.size),
                    declaration.hasAnnotation(composableAnnotations),
                )

            is KtSecondaryConstructor ->
                Part("constructor()", declaration.functionScore(declaration.valueParameters.size), 0.0)

            is KtAnonymousInitializer -> Part("init", declaration.functionScore(0), 0.0)

            else -> null
        }

    fun ofParameter(parameter: KtParameter): Part =
        Part(parameter.nameAsSafeName.asString(), parameter.typeReference.typeScore(), 0.0)

    fun ofNestedClass(name: String): Part =
        Part(name, scoring.nestedClassScore, 0.0)

    private fun KtElement.functionScore(parameters: Int): Double =
        max(1.0, sqrt(parameters.toDouble())) * (scoring.minFunctionScore + CallWeights.total(this, scoring))

    private fun KtTypeReference?.typeScore(): Double =
        if (scoring.arePropertyTypeArgumentsCounted) TypeNodes.count(this?.typeElement).toDouble() else 1.0

    private fun KtProperty.propertyScore(): Double {
        val isAbstract = hasModifier(KtTokens.ABSTRACT_KEYWORD) ||
            (containingClass()?.isInterface() == true && getter?.hasBody() != true)
        return if (isAbstract) 1.0 else typeReference.typeScore() + CallWeights.total(this, scoring)
    }
}
