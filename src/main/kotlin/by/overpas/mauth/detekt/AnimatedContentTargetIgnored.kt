package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.parents

private const val IMPLICIT_PARAMETER = "it"
private const val CONTENT_ARGUMENT = "content"

class AnimatedContentTargetIgnored(config: Config) :
    Rule(
        config,
        "The outgoing and the incoming content are composed together. " +
            "Content that ignores the target of its lambda shows the newest state in both.",
    ) {

    @Configuration("names of the calls whose content lambda receives the target state")
    private val contentSwitchCalls: Set<String> by config(listOf("AnimatedContent", "Crossfade")) { it.toSet() }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val name = expression.calleeName()?.takeIf { it in contentSwitchCalls } ?: return
        val literal = expression.contentLambda()?.functionLiteral
        if (literal != null && !literal.usesTarget()) {
            report(
                Finding(
                    Entity.from(expression),
                    "Render the content of `$name` from the target parameter of its lambda.",
                ),
            )
        }
    }

    private fun KtCallExpression.contentLambda(): KtLambdaExpression? {
        val trailing = lambdaArguments.lastOrNull()?.getLambdaExpression()
        val named = valueArguments.firstOrNull { it.getArgumentName()?.text == CONTENT_ARGUMENT }
        return trailing ?: named?.getArgumentExpression() as? KtLambdaExpression
    }

    private fun KtFunctionLiteral.usesTarget(): Boolean {
        val parameter = valueParameters.firstOrNull()
        val name = parameter?.name ?: IMPLICIT_PARAMETER
        return bodyExpression
            ?.collectDescendantsOfType<KtNameReferenceExpression> { it.getReferencedName() == name }
            .orEmpty()
            .any { parameter != null || it.implicitParameterOwner() == this }
    }

    private fun KtNameReferenceExpression.implicitParameterOwner(): KtFunctionLiteral? =
        parents
            .filterIsInstance<KtFunctionLiteral>()
            .firstOrNull { !it.hasParameterSpecification() }
}
