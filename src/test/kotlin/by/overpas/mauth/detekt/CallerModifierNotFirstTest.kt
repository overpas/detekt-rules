package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class CallerModifierNotFirstTest {

    private val sut = CallerModifierNotFirst(Config.empty)

    @Test
    fun `a chain that starts with the modifier passes`() {
        val code = """
            @Composable
            fun Avatar(url: String, modifier: Modifier = Modifier) {
                Image(
                    modifier = modifier
                        .clip(CircleShape)
                        .size(48.dp),
                )
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a modifier added after intrinsic modifiers is reported`() {
        val code = """
            @Composable
            fun Avatar(url: String, modifier: Modifier = Modifier) {
                Image(
                    modifier = Modifier
                        .clip(CircleShape)
                        .then(modifier),
                )
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `then with another modifier passes`() {
        val code = """
            @Composable
            fun Avatar(selected: Boolean, modifier: Modifier = Modifier) {
                Box(
                    modifier = modifier
                        .then(if (selected) Modifier.background(Color.Red) else Modifier),
                )
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a composable without a modifier parameter passes`() {
        val code = """
            @Composable
            fun Avatar(modifier2: Modifier) {
                Box(modifier = Modifier.then(modifier2))
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a non-composable function passes`() {
        val code = """
            fun avatarModifier(modifier: Modifier): Modifier =
                Modifier.clip(CircleShape).then(modifier)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a custom modifier name is honored`() {
        val sut = CallerModifierNotFirst(TestConfig("modifierName" to "m"))
        val code = """
            @Composable
            fun Avatar(m: Modifier) {
                Box(modifier = Modifier.clip(CircleShape).then(m))
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }
}
