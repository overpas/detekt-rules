package by.overpas.detekt.architecture

import by.overpas.detekt.core.calleeName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtUserType

internal fun KtUserType.nameSegments(): List<String> =
    qualifier?.nameSegments().orEmpty() + listOfNotNull(referencedName)

internal fun KtExpression.nameSegments(): List<String>? =
    when (this) {
        is KtNameReferenceExpression -> listOf(getReferencedName())

        is KtCallExpression -> calleeName()?.let(::listOf)

        is KtDotQualifiedExpression -> receiverExpression.nameSegments()?.let { receiver ->
            selectorExpression?.nameSegments()?.let { receiver + it }
        }

        else -> null
    }
