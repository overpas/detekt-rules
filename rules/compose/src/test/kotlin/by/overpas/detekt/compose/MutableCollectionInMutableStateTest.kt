package by.overpas.detekt.compose

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class MutableCollectionInMutableStateTest {

    private val sut = MutableCollectionInMutableState(Config.empty)

    @Test
    fun `a mutable list factory in a state is reported`() {
        val code = """
            val items = mutableStateOf(mutableListOf<Item>())
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a mutable collection constructor in a state is reported`() {
        val code = """
            val items = mutableStateOf(HashMap<String, Item>())
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a mutable type argument is reported`() {
        val code = """
            val items = mutableStateOf<MutableList<Item>>(source)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a read-only list in a state passes`() {
        val code = """
            val items = mutableStateOf(listOf<Item>())
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a snapshot state list passes`() {
        val code = """
            val items = mutableStateListOf<Item>()
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a custom state factory is honored`() {
        val sut = MutableCollectionInMutableState(TestConfig("stateFactories" to listOf("stateOf")))
        val code = """
            val items = stateOf(mutableListOf<Item>())
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }
}
