package by.overpas.detekt.style

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class ConversionNameMismatchTest {

    private val sut = ConversionNameMismatch(Config.empty)

    @Test
    fun `a to function that returns another type is reported`() {
        val code = """
            class SyncAlert
            class SyncError {
                fun toMessage(): SyncAlert? = null
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an as function that returns another type is reported`() {
        val code = """
            class Account
            fun String.asUser(): Account = Account()
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a to function that returns its type passes`() {
        val code = """
            class GoogleDriveSyncState
            fun Int.toSyncState(): GoogleDriveSyncState = GoogleDriveSyncState()
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a qualified return type passes`() {
        val code = """
            object SyncStore {
                class State
            }
            fun Int.toStoreState(): SyncStore.State = SyncStore.State()
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an or null conversion passes`() {
        val code = """
            fun String.toIntOrNull(): Int? = null
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a type parameter return type passes`() {
        val code = """
            fun <T> Any.asTyped(): T = TODO()
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a function without an explicit return type passes`() {
        val code = """
            class SyncAlert
            fun Int.toMessage() = SyncAlert()
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a function that is not a conversion passes`() {
        val code = """
            fun today(): Int = 0
            fun toggle(): Unit = Unit
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `custom ignored suffixes are honored`() {
        val sut = ConversionNameMismatch(TestConfig("ignoredSuffixes" to emptyList<String>()))
        val code = """
            fun String.toIntOrNull(): Int? = null
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }
}
