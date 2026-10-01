package by.overpas.detekt.compose

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class ReturnInComposableTest {

    private val sut = ReturnInComposable(Config.empty)

    @Test
    fun `a composable without return statements passes`() {
        val code = """
            @Composable
            fun Screen(state: State) {
                if (state.isLoading) {
                    Loader()
                } else {
                    Content(state)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an early return in a composable is reported`() {
        val code = """
            @Composable
            fun Screen(state: State) {
                if (state.isLoading) return
                Content(state)
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an elvis return in a composable is reported`() {
        val code = """
            @Composable
            fun Screen(state: State?) {
                val actual = state ?: return
                Content(actual)
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a labeled return in a content lambda is reported`() {
        val code = """
            @Composable
            fun Screen(state: State) {
                Column {
                    if (state.isLoading) return@Column
                    Content(state)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a return in a composable with an explicit Unit type is reported`() {
        val code = """
            @Composable
            fun Screen(state: State): Unit {
                if (state.isLoading) return
                Content(state)
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a return in a composable that returns a value passes`() {
        val code = """
            @Composable
            fun color(isError: Boolean): Color {
                if (isError) return Color.Red
                return Color.Black
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a return in an expression body composable without a type passes`() {
        val code = """
            @Composable
            fun rememberState() = remember {
                if (isDebug) return@remember DebugState()
                State()
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a return in a non-composable function passes`() {
        val code = """
            fun compute(state: State?) {
                if (state == null) return
                state.compute()
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a return in a nested function of a composable passes`() {
        val code = """
            @Composable
            fun Screen(state: State) {
                fun validate(input: String) {
                    if (input.isEmpty()) return
                    state.submit(input)
                }
                val onSubmit = fun(input: String) {
                    if (input.isEmpty()) return
                    state.submit(input)
                }
                Content(state, ::validate, onSubmit)
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `each return in a composable is reported`() {
        val code = """
            @Composable
            fun Screen(state: State) {
                if (state.isLoading) return
                if (state.isEmpty) return
                Content(state)
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(2, findings.size)
    }

    @Test
    fun `a custom composable annotation is honored`() {
        val sut = ReturnInComposable(TestConfig("composableAnnotations" to listOf("Widget")))
        val code = """
            @Widget
            fun Screen(state: State) {
                if (state.isLoading) return
                Content(state)
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }
}
