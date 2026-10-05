package by.overpas.detekt.compose

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class LowLevelUiPrimitiveTest {

    private val sut = LowLevelUiPrimitive(Config.empty)

    @Test
    fun `an import of a forbidden name is reported`() {
        val code = """
            import androidx.compose.ui.unit.dp
        """.trimIndent()
        val expected = listOf("import androidx.compose.ui.unit.dp")

        val findings = sut.lint(code)
        val reported = findings.map { it.entity.ktElement.text }

        assertEquals(expected, reported)
    }

    @Test
    fun `an import from a forbidden package is reported`() {
        val code = """
            import androidx.compose.ui.graphics.Color
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an import from a subpackage of a forbidden package is reported`() {
        val code = """
            import androidx.compose.ui.graphics.drawscope.Stroke
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a star import of a forbidden package is reported`() {
        val code = """
            import androidx.compose.ui.text.style.*
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an import that matches a name prefix pattern is reported`() {
        val code = """
            import androidx.compose.animation.fadeIn
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `imports of components and layout containers pass`() {
        val code = """
            import androidx.compose.animation.AnimatedVisibility
            import androidx.compose.foundation.layout.Column
            import androidx.compose.foundation.layout.fillMaxWidth
            import androidx.compose.material3.Button
            import androidx.compose.ui.Modifier
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an allowed import from a forbidden package passes`() {
        val code = """
            import androidx.compose.ui.graphics.vector.ImageVector
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a fully qualified reference in an expression is reported once`() {
        val code = """
            val color = androidx.compose.ui.graphics.Color.Red.copy(alpha = 0.5f)
        """.trimIndent()
        val expected = listOf("androidx.compose.ui.graphics.Color.Red.copy(alpha = 0.5f)")

        val findings = sut.lint(code)
        val reported = findings.map { it.entity.ktElement.text }

        assertEquals(expected, reported)
    }

    @Test
    fun `a fully qualified call is reported`() {
        val code = """
            fun Screen() {
                androidx.compose.foundation.Canvas(modifier = Modifier) {}
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a fully qualified type is reported`() {
        val code = """
            fun Title(style: androidx.compose.ui.text.TextStyle) {}
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a qualified expression that is not a forbidden name passes`() {
        val code = """
            fun Screen(state: State) {
                Modifier.fillMaxWidth().testTag(state.tag)
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a package that matches a forbidden pattern passes`() {
        val code = """
            package androidx.compose.ui.graphics.sample

            val color = 0
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `custom forbidden imports are honored`() {
        val sut = LowLevelUiPrimitive(
            TestConfig("forbiddenImports" to listOf("androidx.compose.ui.text.input.*")),
        )
        val code = """
            import androidx.compose.ui.text.input.KeyboardType
            import androidx.compose.ui.unit.dp
        """.trimIndent()
        val expected = listOf("import androidx.compose.ui.text.input.KeyboardType")

        val findings = sut.lint(code)
        val reported = findings.map { it.entity.ktElement.text }

        assertEquals(expected, reported)
    }

    @Test
    fun `custom allowed imports are honored`() {
        val sut = LowLevelUiPrimitive(
            TestConfig("allowedImports" to listOf("androidx.compose.ui.unit.*")),
        )
        val code = """
            import androidx.compose.ui.unit.dp
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }
}
