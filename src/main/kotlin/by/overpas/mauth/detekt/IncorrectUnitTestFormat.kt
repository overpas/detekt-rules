package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNamedFunction

class IncorrectUnitTestFormat(config: Config) :
    Rule(
        config,
        "A unit test body must be separated by empty lines into an optional arrange block, " +
            "an act block and a final assert block.",
    ) {

    @Configuration("short names of the annotations that mark a unit test")
    private val testAnnotations: Set<String> by config(listOf("Test")) { it.toSet() }

    @Configuration("name prefixes of the calls that count as an assertion")
    private val assertionPrefixes: List<String> by config(listOf("assert", "verify", "fail"))

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        if (!function.isUnitTest(testAnnotations)) return
        function.testBody(assertionPrefixes)
            ?.blocks()
            ?.formatError()
            ?.let { report(Finding(Entity.atName(function), it)) }
    }

    private fun List<List<KtExpression>>.formatError(): String? =
        when {
            size < MIN_BLOCK_COUNT ->
                "An act block and an assert block must be separated by an empty line."

            size > MAX_BLOCK_COUNT ->
                "There must be no more than $MAX_BLOCK_COUNT blocks: arrange, act and assert."

            else -> null
        }
}
