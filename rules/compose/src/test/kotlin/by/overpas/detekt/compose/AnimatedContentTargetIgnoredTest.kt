package by.overpas.detekt.compose

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class AnimatedContentTargetIgnoredTest {

    private val sut = AnimatedContentTargetIgnored(Config.empty)

    @Test
    fun `content that uses the named target passes`() {
        val code = """
            @Composable
            fun Screen(selectedId: String) {
                AnimatedContent(targetState = selectedId) { targetId ->
                    Destination(targetId)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `content that uses the implicit target passes`() {
        val code = """
            @Composable
            fun Screen(selectedId: String) {
                Crossfade(targetState = selectedId) {
                    Destination(it)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `content that captures the outer state is reported`() {
        val code = """
            @Composable
            fun Screen(selectedId: String) {
                AnimatedContent(targetState = selectedId) {
                    Destination(selectedId)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an unused named target is reported`() {
        val code = """
            @Composable
            fun Screen(selectedId: String) {
                AnimatedContent(targetState = selectedId) { targetId ->
                    Destination(selectedId)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an implicit target of a nested lambda is reported`() {
        val code = """
            @Composable
            fun Screen(items: List<Item>) {
                AnimatedContent(targetState = items.size) {
                    items.forEach { Row(it) }
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a named content argument is checked`() {
        val code = """
            @Composable
            fun Screen(selectedId: String) {
                Crossfade(targetState = selectedId, content = { Destination(selectedId) })
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a custom content switch call is honored`() {
        val sut = AnimatedContentTargetIgnored(TestConfig("contentSwitchCalls" to listOf("Switcher")))
        val code = """
            @Composable
            fun Screen(selectedId: String) {
                Switcher(selectedId) {
                    Destination(selectedId)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }
}
