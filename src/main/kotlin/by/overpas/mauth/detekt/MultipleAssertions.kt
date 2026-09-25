package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtNamedFunction

class MultipleAssertions(config: Config) :
    Rule(
        config,
        "The final assert block of a unit test must contain exactly one assertion. " +
            "Group related checks with one assertOn call.",
    ) {

    @Configuration("short names of the annotations that mark a unit test")
    private val testAnnotations: Set<String> by config(listOf("Test")) { it.toSet() }

    @Configuration("name prefixes of the calls that count as an assertion")
    private val assertionPrefixes: List<String> by config(listOf("assert", "verify", "fail"))

    private val assertions by lazy { Assertions(assertionPrefixes) }

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        if (!function.isUnitTest(testAnnotations)) return
        val assertionCount = assertions.testBody(function)
            ?.let { body -> body.blocks().takeIf { it.size in MIN_BLOCK_COUNT..MAX_BLOCK_COUNT } }
            ?.let { blocks -> blocks.last().count { assertions.isAssertion(it) } }
            ?: return
        if (assertionCount > 1) {
            report(
                Finding(
                    Entity.atName(function),
                    "The last block contains $assertionCount assertions. " +
                        "Keep one assertion, or group the checks with one assertOn call.",
                ),
            )
        }
    }
}
