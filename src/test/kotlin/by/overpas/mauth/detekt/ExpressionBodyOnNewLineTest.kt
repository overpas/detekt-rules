package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class ExpressionBodyOnNewLineTest {

    private val sut = ExpressionBodyOnNewLine(Config.empty)

    @Test
    fun `a one-line function is reported`() {
        val code = """
            fun answer(): Int = 42
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a one-line member function is reported`() {
        val code = """
            class Calculator {
                fun answer(): Int = 42
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a one-line local function is reported`() {
        val code = """
            fun outer(): Int {
                fun inner(): Int = 42
                return inner()
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a when on the signature line is reported`() {
        val code = """
            fun sign(value: Int): String = when {
                value < 0 -> "-"
                else -> "+"
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a body on the next line passes`() {
        val code = """
            fun answer(): Int =
                42
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a body after a multi-line signature passes`() {
        val code = """
            fun sum(
                first: Int,
                second: Int,
            ): Int =
                first + second
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a block body passes`() {
        val code = """
            fun answer(): Int {
                return 42
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a function without a body passes`() {
        val code = """
            interface Source {
                fun answer(): Int
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }
}
