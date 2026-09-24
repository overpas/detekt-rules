package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class FalseStabilityPromiseTest {

    private val sut = FalseStabilityPromise(Config.empty)

    @Test
    fun `an immutable class with read-only properties passes`() {
        val code = """
            @Immutable
            data class State(val name: String, val items: List<Item>)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a var in an immutable class is reported`() {
        val code = """
            @Immutable
            class State(var name: String)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a snapshot state var in an immutable class is reported`() {
        val code = """
            @Immutable
            class State {
                var name by mutableStateOf("")
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a mutable collection in an immutable class is reported`() {
        val code = """
            @Immutable
            data class State(val items: MutableList<Item>?)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a snapshot state var in a stable class passes`() {
        val code = """
            @Stable
            class State {
                var name by mutableStateOf("")
                var count by mutableIntStateOf(0)
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a plain var in a stable class is reported`() {
        val code = """
            @Stable
            class State {
                var name = ""
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a mutable collection factory in a stable class is reported`() {
        val code = """
            @Stable
            class State {
                val items = mutableListOf<Item>()
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a class without a stability annotation passes`() {
        val code = """
            class State(var name: String)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a custom immutability annotation is honored`() {
        val sut = FalseStabilityPromise(TestConfig("immutableAnnotations" to listOf("Frozen")))
        val code = """
            @Frozen
            class State(var name: String)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }
}
