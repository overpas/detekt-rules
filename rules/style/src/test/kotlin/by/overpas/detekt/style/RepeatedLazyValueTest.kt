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
            class Settings {
                fun get(key: String): String = key
            }
            class SyncStatus(private val settings: Lazy<Settings>) {
                fun lastSync() = settings.value.get("last")
                fun error() = settings.value.get("error")
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a value read through this is counted`() {
        val code = """
            class SyncStatus(private val settings: Lazy<String>) {
                fun lastSync() = this.settings.value
                fun error() = settings.value
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
            class SyncStatus {
                private val settings: Lazy<String> = lazy { "settings" }
                fun lastSync() = settings.value
                fun error() = settings.value
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
            class SyncStatus(settings: Lazy<String>) {
                private val name by settings
                fun lastSync() = name
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a value read once into a private value passes`() {
        val code = """
            class SyncStatus(settings: Lazy<String>) {
                private val name = settings.value
                fun lastSync() = name
                fun error() = name
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a value of another type read twice passes`() {
        val code = """
            class Box(val value: String)
            class SyncStatus(private val box: Box) {
                fun first() = box.value
                fun second() = box.value
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }
}
