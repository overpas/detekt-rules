package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS

class TypeCast(config: Config) :
    Rule(
        config,
        "A type cast skips the compiler type check. Use a smart cast or a typed API instead.",
    ) {

    override fun visitBinaryWithTypeRHSExpression(expression: KtBinaryExpressionWithTypeRHS) {
        super.visitBinaryWithTypeRHSExpression(expression)
        val operator = expression.operationReference.text
        val type = expression.right?.text ?: return
        report(Finding(Entity.from(expression), "Remove the `$operator $type` cast."))
    }
}
