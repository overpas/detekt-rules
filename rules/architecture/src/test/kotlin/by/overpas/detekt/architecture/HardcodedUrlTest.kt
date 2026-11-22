package by.overpas.detekt.architecture

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class HardcodedUrlTest {

    private val sut = HardcodedUrl(Config.empty)

    @Test
    fun `an https url is reported`() {
        val code = """
            package app.auth

            class UserInfoApi {
                private val url = "https://openidconnect.googleapis.com/v1/userinfo"
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an http url inside a template is reported`() {
        val code = """
            package app.auth

            fun endpoint(path: String) = "http://localhost:8080/${'$'}path"
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a url in a di package passes`() {
        val code = """
            package app.auth.di

            object AuthUrls {
                const val USER_INFO = "https://openidconnect.googleapis.com/v1/userinfo"
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a string without a url passes`() {
        val code = """
            package app.auth

            val scheme = "https"
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `custom allowed packages are honored`() {
        val sut = HardcodedUrl(TestConfig("allowedPackages" to listOf("config")))
        val code = """
            package app.config

            val userInfo = "https://openidconnect.googleapis.com/v1/userinfo"
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }
}
