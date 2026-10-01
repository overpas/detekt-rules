package by.overpas.detekt.style

import com.intellij.psi.PsiWhiteSpace
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.siblings

class ExpressionBodyOnNewLine(config: Config) :
    Rule(
        config,
        "An expression body on the signature line is hard to scan. Put a newline after `=`.",
    ) {

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        val equalsToken = function.equalsToken ?: return
        val body = function.bodyExpression ?: return
        val isOnSignatureLine = equalsToken.siblings(withItself = false)
            .takeWhile { it != body }
            .none { it is PsiWhiteSpace && '\n' in it.text }
        if (isOnSignatureLine) {
            report(
                Finding(Entity.from(function), "Put a newline after `=` in `${function.nameAsSafeName}`."),
            )
        }
    }
}
