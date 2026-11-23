package by.overpas.detekt.style

import dev.detekt.api.Config
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class RepeatedLazyValueTest {

    private val sut = RepeatedLazyValue(Config.empty)

    @Test
    fun `a constructor parameter whose value is read twice is reported`() {
        val code = """
            class Preferences {
                fun get(key: String): String = key
            }
            class ThemeRepository(private val preferences: Lazy<Preferences>) {
                fun accent() = preferences.value.get("accent")
                fun fontScale() = preferences.value.get("font_scale")
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a value read through this is counted`() {
        val code = """
            class ThemeRepository(private val preferences: Lazy<String>) {
                fun accent() = this.preferences.value
                fun fontScale() = preferences.value
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a function parameter whose value is read twice is reported`() {
        val code = """
            fun describe(name: Lazy<String>): String = name.value + name.value
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a property whose value is read twice is reported`() {
        val code = """
            class ThemeRepository {
                private val preferences: Lazy<String> = lazy { "preferences" }
                fun accent() = preferences.value
                fun fontScale() = preferences.value
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a local value whose value is read twice is reported`() {
        val code = """
            fun describe(): String {
                val name: Lazy<String> = lazy { "name" }
                return name.value + name.value
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a value read once passes`() {
        val code = """
            class ThemeRepository(preferences: Lazy<String>) {
                private val name by preferences
                fun accent() = name
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a value read once into a private value passes`() {
        val code = """
            class ThemeRepository(preferences: Lazy<String>) {
                private val name = preferences.value
                fun accent() = name
                fun fontScale() = name
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a value of another type read twice passes`() {
        val code = """
            class Box(val value: String)
            class ThemeRepository(private val box: Box) {
                fun first() = box.value
                fun second() = box.value
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `reads of a shadowing class property are not counted for a top level value`() {
        val code = """
            val preferences: Lazy<String> = lazy { "" }
            val accent = preferences.value

            class ThemeRepository(private val preferences: Lazy<String>) {
                fun fontScale() = preferences.value
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `reads of a shadowing lambda parameter are not counted`() {
        val code = """
            fun render(preferences: Lazy<String>, other: Lazy<String>) {
                println(preferences.value)
                listOf(other).forEach { preferences: Lazy<String> -> println(preferences.value) }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }
}
