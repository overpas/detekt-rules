package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNamedFunction

class MisplacedAssertion(config: Config) :
    Rule(
        config,
        "An assertion is only allowed in the final assert block of a unit test body. " +
            "No other call is allowed in that block.",
    ) {

    @Configuration("short names of the annotations that mark a unit test")
    private val testAnnotations: Set<String> by config(listOf("Test")) { it.toSet() }

    @Configuration("name prefixes of the calls that count as an assertion")
    private val assertionPrefixes: List<String> by config(listOf("assert", "verify", "fail"))

    private val assertions by lazy { Assertions(assertionPrefixes) }

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        if (!function.isUnitTest(testAnnotations)) return
        assertions.testBody(function)
            ?.blocks()
            ?.takeIf { it.size in MIN_BLOCK_COUNT..MAX_BLOCK_COUNT }
            ?.placementError()
            ?.let { report(Finding(Entity.atName(function), it)) }
    }

    private fun List<List<KtExpression>>.placementError(): String? =
        when {
            last().any { !assertions.isAssertion(it) } ->
                "The last block must contain assertions only."

            dropLast(1).any { block -> block.any { assertions.isAssertion(it) } } ->
                "An assertion is only allowed in the last block."

            else -> null
        }
}
