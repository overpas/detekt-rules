package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class RedundantFunctionNameTest {

    private val sut = RedundantFunctionName(Config.empty)

    @Test
    fun `a get function passes`() {
        val code = """
            class UserRepository {
                fun get(id: String) = Unit
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a get by id function passes`() {
        val code = """
            class UserRepository {
                fun getById(id: String) = Unit
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a get all function passes`() {
        val code = """
            class UserRepository {
                fun getAll() = Unit
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a word that starts with the subject passes`() {
        val code = """
            class UserRepository {
                fun getUsername() = Unit
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an override function passes`() {
        val code = """
            class UserRepository : Repository {
                override fun getUser(id: String) = Unit
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a companion object function passes`() {
        val code = """
            class UserRepository {
                companion object {
                    fun getUser(id: String) = Unit
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a top level function passes`() {
        val code = """
            fun getUser(id: String) = Unit
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a class function that repeats the subject is reported`() {
        val code = """
            class UserRepository {
                fun getUser(id: String) = Unit
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an interface function that repeats the subject is reported`() {
        val code = """
            interface UserRepository {
                fun getUser(id: String)
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a plural of the subject is reported`() {
        val code = """
            class UserRepository {
                fun getUsers() = Unit
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `the last word of a multi word subject is reported`() {
        val code = """
            class OtpEntryRepository {
                fun getEntry(id: String) = Unit
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a multi word subject is reported`() {
        val code = """
            class OtpEntryRepository {
                fun getOtpEntry(id: String) = Unit
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `the name of a one word class is reported`() {
        val code = """
            class User {
                fun copyUser() = Unit
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an ignored word passes`() {
        val code = """
            class PreviewInTest {
                fun isInDirective() = Unit
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `the ignored words are configurable`() {
        val sut = RedundantFunctionName(TestConfig("ignoredWords" to listOf("User")))
        val code = """
            class UserRepository {
                fun getUser(id: String) = Unit
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }
}
