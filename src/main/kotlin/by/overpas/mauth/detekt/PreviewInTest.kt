package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class PreviewInTest(config: Config) :
    Rule(
        config,
        "A preview class or function shows sample data for the IDE and can change at any time. " +
            "Tests must use fakes that they own.",
    ) {

    @Configuration("regex that the name of a forbidden class or function contains")
    private val namePattern: Regex by config("[Pp]review") { it.toRegex() }

    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
        super.visitSimpleNameExpression(expression)
        val name = expression.getReferencedName()
        if (!namePattern.containsMatchIn(name) || expression.isInDirective()) return
        report(Finding(Entity.from(expression), "Replace `$name` with a fake that the test owns."))
    }

    private fun KtSimpleNameExpression.isInDirective(): Boolean =
        getStrictParentOfType<KtImportDirective>() != null || getStrictParentOfType<KtPackageDirective>() != null
}
