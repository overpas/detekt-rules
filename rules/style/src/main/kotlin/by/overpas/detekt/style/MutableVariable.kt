package by.overpas.detekt.style

import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.parents

class MutableVariable(config: Config) :
    Rule(
        config,
        "A mutable variable makes the state hard to follow. Use val instead.",
    ) {

    @Configuration("short names of the annotations that mark a composable function")
    private val composableAnnotations: Set<String> by config(listOf("Composable")) { it.toSet() }

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)
        if (!property.isVar || property.hasModifier(KtTokens.LATEINIT_KEYWORD) || property.isInComposable()) return
        report(Finding(Entity.from(property), "Replace the var `${property.nameAsSafeName}` with a val."))
    }

    private fun KtProperty.isInComposable(): Boolean =
        parents.filterIsInstance<KtNamedFunction>().any { function ->
            function.annotationEntries.any { it.shortName?.asString() in composableAnnotations }
        }
}
