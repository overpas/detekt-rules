package by.overpas.detekt.compose

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class RequestFocusInCompositionTest {

    private val sut = RequestFocusInComposition(Config.empty)

    @Test
    fun `a request in the composable body is reported`() {
        val code = """
            @Composable
            fun Screen() {
                val requester = remember { FocusRequester() }
                requester.requestFocus()
                Button(modifier = Modifier.focusRequester(requester))
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a request in composable content is reported`() {
        val code = """
            @Composable
            fun Screen(requester: FocusRequester) {
                Column {
                    requester.requestFocus()
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a request in LaunchedEffect passes`() {
        val code = """
            @Composable
            fun Screen(requester: FocusRequester) {
                LaunchedEffect(requester) {
                    requester.requestFocus()
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a request in an event callback passes`() {
        val code = """
            @Composable
            fun Screen(requester: FocusRequester) {
                Button(onClick = { requester.requestFocus() }) {
                    Text("Focus")
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a request in a non-composable function passes`() {
        val code = """
            fun focus(requester: FocusRequester) {
                requester.requestFocus()
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a custom effect call is honored`() {
        val sut = RequestFocusInComposition(TestConfig("effectCalls" to listOf("OnAppear")))
        val code = """
            @Composable
            fun Screen(requester: FocusRequester) {
                OnAppear {
                    requester.requestFocus()
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }
}
