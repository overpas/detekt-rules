package by.overpas.detekt.gradle

import by.overpas.detekt.core.calleeName
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

private const val PLUGIN_ALIAS = "alias"

internal data class DependencyDeclaration(
    val statement: KtExpression,
    val configuration: String,
    val notation: String,
)

internal fun KtCallExpression.lambdaBody(): KtBlockExpression? =
    lambdaArguments.singleOrNull()?.getLambdaExpression()?.bodyExpression

internal fun KtExpression.pluginCall(): KtCallExpression? =
    when (this) {
        is KtBinaryExpression -> left?.pluginCall()
        is KtCallExpression -> this
        else -> null
    }

internal fun KtCallExpression.pluginKey(): Pair<Int, String> {
    val group = if (calleeName() == PLUGIN_ALIAS) 1 else 0
    return group to firstArgumentText()
}

internal fun KtExpression.dependencyBlockName(): String? {
    val selector = (this as? KtDotQualifiedExpression)?.selectorExpression as? KtCallExpression
    if (selector?.calleeName() != "dependencies") return null
    return when (val receiver = receiverExpression) {
        is KtNameReferenceExpression -> receiver.getReferencedName()
        is KtCallExpression -> receiver.firstArgumentText()
        else -> null
    }
}

internal fun KtExpression.dependencyDeclaration(): DependencyDeclaration? {
    val configuration = (this as? KtCallExpression)
        ?.takeIf { it.lambdaArguments.isEmpty() }
        ?.calleeName()
    return configuration?.let {
        DependencyDeclaration(this, it, firstArgumentText())
    }
}

internal fun String.isTestLibrary(testLibraries: List<String>): Boolean =
    testLibraries.any { this == it || startsWith("$it.") }

private fun KtCallExpression.firstArgumentText(): String =
    valueArguments
        .firstOrNull()
        ?.getArgumentExpression()
        ?.text
        .orEmpty()
        .trim('"')
