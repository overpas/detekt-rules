package by.overpas.detekt.style

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class MutableVariableTest {

    private val sut = MutableVariable(Config.empty)

    @Test
    fun `a local var is reported`() {
        val code = """
            fun count(): Int {
                var total = 0
                total += 1
                return total
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a member var is reported`() {
        val code = """
            class Counter {
                var value = 0
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a top-level var is reported`() {
        val code = """
            var counter = 0
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a delegated var outside a composable is reported`() {
        val code = """
            fun load() {
                var value by lazyState()
                value = 1
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a val of a mutable collection passes`() {
        val code = """
            fun collect(): List<Int> {
                val items = mutableListOf<Int>()
                items.add(1)
                return items
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a lateinit var passes`() {
        val code = """
            class Holder {
                lateinit var value: String
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a var in a composable passes`() {
        val code = """
            @Composable
            fun Dropdown() {
                var expanded by remember { mutableStateOf(false) }
                Menu(expanded) { expanded = it }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a var in a local function of a composable passes`() {
        val code = """
            @Composable
            fun Screen() {
                fun count(): Int {
                    var total = 0
                    total += 1
                    return total
                }
                Text(count())
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a var in a custom composable annotation passes`() {
        val sut = MutableVariable(TestConfig("composableAnnotations" to listOf("Widget")))
        val code = """
            @Widget
            fun Dropdown() {
                var expanded by remember { mutableStateOf(false) }
                Menu(expanded) { expanded = it }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }
}
