package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class ModifierChainWrappingTest {

    private val sut = ModifierChainWrapping(Config.empty)

    @Test
    fun `two calls on one line pass`() {
        val code = """
            val modifier = Modifier.fillMaxWidth().padding(8.dp)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `three calls on one line are reported`() {
        val code = """
            val modifier = Modifier.fillMaxWidth().padding(8.dp).clip(CircleShape)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `three calls one per line pass`() {
        val code = """
            val modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clip(CircleShape)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a first call on the start line is reported`() {
        val code = """
            val modifier = modifier.fillMaxWidth()
                .padding(8.dp)
                .clip(CircleShape)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a chain that does not start with a modifier passes`() {
        val code = """
            val names = items.map { it.name }.filter { it.isNotEmpty() }.sorted()
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a nested chain is checked separately`() {
        val code = """
            val modifier = Modifier
                .fillMaxWidth()
                .then(Modifier.padding(8.dp).clip(CircleShape).background(Color.Red))
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a custom minimum call count is honored`() {
        val sut = ModifierChainWrapping(TestConfig("minCallCount" to 2))
        val code = """
            val modifier = Modifier.fillMaxWidth().padding(8.dp)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }
}
