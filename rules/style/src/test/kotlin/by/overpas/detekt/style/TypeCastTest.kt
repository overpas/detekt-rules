package by.overpas.detekt.style

import dev.detekt.api.Config
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class TypeCastTest {

    private val sut = TypeCast(Config.empty)

    @Test
    fun `an unsafe cast is reported`() {
        val code = """
            fun name(value: Any): String = value as String
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a safe cast is reported`() {
        val code = """
            fun name(value: Any): String? = value as? String
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a type check passes`() {
        val code = """
            fun length(value: Any): Int = if (value is String) value.length else 0
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an import alias passes`() {
        val code = """
            import kotlin.collections.List as Items

            fun empty(): Items<Int> = emptyList()
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }
}
