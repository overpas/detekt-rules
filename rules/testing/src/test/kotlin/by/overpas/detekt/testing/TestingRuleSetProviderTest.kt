package by.overpas.detekt.testing

import kotlin.test.Test
import kotlin.test.assertEquals

class TestingRuleSetProviderTest {

    private val sut = TestingRuleSetProvider()

    @Test
    fun `default config is under the rule set id`() {
        val header = checkNotNull(javaClass.getResource("/config/config.yml")).readText()
            .lines()
            .first()

        val ruleSetId = sut.instance().id.value

        assertEquals("$ruleSetId:", header)
    }

    @Test
    fun `default config lists every rule of the rule set`() {
        val ruleEntry = Regex("^ {2}(\\w+):$")
        val configured = checkNotNull(javaClass.getResource("/config/config.yml")).readText()
            .lines()
            .mapNotNull { line -> ruleEntry.find(line)?.run { groupValues[1] } }
            .toSet()

        val ruleNames = sut.instance().rules.keys
            .map { it.value }
            .toSet()

        assertEquals(configured, ruleNames)
    }
}
