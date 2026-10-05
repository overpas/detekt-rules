package by.overpas.detekt.compose

import by.overpas.detekt.core.calleeName
import com.intellij.psi.PsiElement
import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.parents

private const val WILDCARD = "*"

class LowLevelUiPrimitive(config: Config) :
    Rule(
        config,
        "UI code combines design system components and does not build their look from low-level " +
            "primitives: colors, typography, dimensions, shapes, drawing, custom layouts or animation specs. " +
            "Put the look in the design system.",
    ) {

    @Configuration("patterns of the fully qualified names that are low-level UI primitives, `*` matches any characters")
    private val forbiddenImports: List<Regex> by config(
        listOf(
            "androidx.compose.ui.graphics.*",
            "androidx.compose.ui.draw.*",
            "androidx.compose.foundation.Canvas",
            "androidx.compose.foundation.background",
            "androidx.compose.foundation.border",
            "androidx.compose.foundation.shape.*",
            "androidx.compose.ui.text.TextStyle",
            "androidx.compose.ui.text.font.*",
            "androidx.compose.ui.text.style.*",
            "androidx.compose.ui.unit.dp",
            "androidx.compose.ui.unit.sp",
            "androidx.compose.ui.unit.em",
            "androidx.compose.ui.layout.*",
            "androidx.compose.animation.core.*",
            "androidx.compose.animation.fade*",
            "androidx.compose.animation.slide*",
            "androidx.compose.animation.expand*",
            "androidx.compose.animation.shrink*",
            "androidx.compose.animation.scale*",
        ),
    ) { patterns -> patterns.map(::toRegex) }

    @Configuration("patterns of the fully qualified names that are allowed although they match `forbiddenImports`")
    private val allowedImports: List<Regex> by config(
        listOf(
            "androidx.compose.ui.graphics.vector.ImageVector",
            "androidx.compose.ui.graphics.painter.Painter",
        ),
    ) { patterns -> patterns.map(::toRegex) }

    override fun visitImportDirective(importDirective: KtImportDirective) {
        super.visitImportDirective(importDirective)
        val importedName = importDirective.importedFqName?.asString() ?: return
        val name = if (importDirective.isAllUnder) "$importedName.$WILDCARD" else importedName
        if (name.isLowLevel()) report(importDirective, name)
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)
        if (expression.parent is KtDotQualifiedExpression || expression.isInHeader()) return
        expression.qualifiedNameSegments().lowLevelPrefix()?.let { report(expression, it) }
    }

    override fun visitUserType(type: KtUserType) {
        super.visitUserType(type)
        if (type.parent is KtUserType) return
        generateSequence(type) { it.qualifier }
            .map { it.referencedName }
            .toList()
            .reversed()
            .takeWhile { it != null }
            .filterNotNull()
            .lowLevelPrefix()
            ?.let { report(type, it) }
    }

    private fun KtDotQualifiedExpression.qualifiedNameSegments(): List<String> {
        val parts = generateSequence<KtExpression>(this) { (it as? KtDotQualifiedExpression)?.receiverExpression }
            .map { (it as? KtDotQualifiedExpression)?.selectorExpression ?: it }
            .toList()
            .asReversed()
        val names = parts
            .asSequence()
            .map { (it as? KtNameReferenceExpression)?.getReferencedName() }
            .takeWhile { it != null }
            .filterNotNull()
            .toList()
        val callName = (parts.getOrNull(names.size) as? KtCallExpression)?.calleeName()
        return names + listOfNotNull(callName)
    }

    private fun List<String>.lowLevelPrefix(): String? =
        runningReduce { prefix, segment -> "$prefix.$segment" }
            .drop(1)
            .firstOrNull { it.isLowLevel() }

    private fun String.isLowLevel(): Boolean =
        forbiddenImports.any { it.matches(this) } && allowedImports.none { it.matches(this) }

    private fun PsiElement.isInHeader(): Boolean =
        parents.any { it is KtImportDirective || it is KtPackageDirective }

    private fun report(
        element: PsiElement,
        name: String,
    ) {
        report(
            Finding(
                Entity.from(element),
                "`$name` is a low-level UI primitive. Use a design system component or token instead.",
            ),
        )
    }
}

private fun toRegex(pattern: String): Regex =
    Regex(pattern.split(WILDCARD).joinToString(".*") { Regex.escape(it) })
