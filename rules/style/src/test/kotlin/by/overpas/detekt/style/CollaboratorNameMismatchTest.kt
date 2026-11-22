package by.overpas.detekt.style

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals

class CollaboratorNameMismatchTest {

    private val environment = createEnvironment()

    private val sut = CollaboratorNameMismatch(Config.empty)

    @Test
    fun `a name without the last word of its type is reported`() {
        val code = """
            interface NetworkClient
            class UserApi(private val network: NetworkClient)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a name after the first word of its type is reported`() {
        val code = """
            interface DriveBackupRepository
            class Sync(private val backups: DriveBackupRepository)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a name with the last word of its type passes`() {
        val code = """
            interface NetworkClient
            interface OtpDb
            class UserApi(private val client: NetworkClient, private val remoteOtpDb: OtpDb?)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a name with an acronym of its type passes`() {
        val code = """
            interface OAuthClient
            class SignIn(private val oAuthClient: OAuthClient)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `primitive, stdlib and function types pass`() {
        val code = """
            class Settings(
                private val retries: Int,
                private val names: List<String>,
                private val onChange: () -> Unit,
                private val onClick: (() -> Unit)?,
            )
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a type parameter passes`() {
        val code = """
            class Box<T>(private val content: T)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a constructor parameter that is not a property passes`() {
        val code = """
            interface NetworkClient
            class UserApi(network: NetworkClient)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a data class passes`() {
        val code = """
            class GoogleAccount
            data class Session(val email: GoogleAccount)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a data class is reported when data classes are not ignored`() {
        val sut = CollaboratorNameMismatch(TestConfig("areDataClassesIgnored" to false))
        val code = """
            class GoogleAccount
            data class Session(val email: GoogleAccount)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a type from a package that is not excluded is reported`() {
        val sut = CollaboratorNameMismatch(TestConfig("excludedPackages" to emptyList<String>()))
        val code = """
            class Settings(private val retries: Int)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }
}
