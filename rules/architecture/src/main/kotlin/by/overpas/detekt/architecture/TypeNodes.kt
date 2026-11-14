package by.overpas.detekt.architecture

import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtTypeElement
import org.jetbrains.kotlin.psi.KtUserType

internal object TypeNodes {

    fun count(element: KtTypeElement?): Int =
        when (element) {
            is KtNullableType -> count(element.innerType)

            is KtUserType -> 1 + element.typeArguments.sumOf { count(it.typeReference?.typeElement) }

            is KtFunctionType ->
                1 +
                    listOfNotNull(element.receiverTypeReference, element.returnTypeReference)
                        .plus(element.parameters.mapNotNull { it.typeReference })
                        .sumOf { count(it.typeElement) }

            else -> 1
        }
}
