package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class SubjectlessWhenOnOneValueTest {

    private val sut = SubjectlessWhenOnOneValue(Config.empty)

    @Test
    fun `type checks on one value are reported`() {
        val code = """
            fun label(event: Event): String =
                when {
                    event is Event.Message -> "message"
                    event is Event.Empty -> "empty"
                    else -> "other"
                }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `equality and range checks on one value are reported`() {
        val code = """
            fun label(code: Int): String =
                when {
                    code == 0 -> "zero"
                    code in 1..9 -> "digit"
                    else -> "number"
                }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `checks on different values pass`() {
        val code = """
            fun label(algorithm: Algorithm?, type: String): String =
                when {
                    algorithm == null -> "none"
                    type == "totp" -> "totp"
                    else -> "hotp"
                }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a boolean condition passes`() {
        val code = """
            fun label(input: String): String =
                when {
                    input.isEmpty() -> "empty"
                    input == "0" -> "zero"
                    else -> "value"
                }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a single branch passes`() {
        val code = """
            fun label(code: Int): String =
                when {
                    code == 0 -> "zero"
                    else -> "number"
                }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a when with a subject passes`() {
        val code = """
            fun label(code: Int): String =
                when (code) {
                    0 -> "zero"
                    1 -> "one"
                    else -> "number"
                }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }
}
