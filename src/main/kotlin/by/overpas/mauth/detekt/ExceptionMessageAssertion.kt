package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

class ExceptionMessageAssertion(config: Config) :
    Rule(
        config,
        "A unit test must not read the message of an exception. " +
            "An exception message is not a contract.",
    ) {

    @Configuration("short names of the annotations that mark a unit test")
    private val testAnnotations: Set<String> by config(listOf("Test")) { it.toSet() }

    @Configuration("names of the exception accessors that a unit test must not read")
    private val exceptionAccessors: Set<String> by config(
        listOf("message", "localizedMessage", "stackTraceToString"),
    ) { it.toSet() }

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        if (!function.isUnitTest(testAnnotations)) return
        function.collectDescendantsOfType<KtQualifiedExpression>()
            .mapNotNull { it.selectorExpression }
            .filter { it.accessorName() in exceptionAccessors }
            .forEach { report(Finding(Entity.from(it), it.findingMessage())) }
    }

    private fun KtExpression.accessorName(): String? =
        (this as? KtNameReferenceExpression)?.getReferencedName() ?: outermostCall()?.calleeName()

    private fun KtExpression.findingMessage(): String = "Do not read `${accessorName().orEmpty()}` in a unit test. " +
        "An exception message is not a contract; assert the exception type instead."
}
