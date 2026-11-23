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
            package com.example.shop

            class OrderApi {
                private val url = "https://api.example.com/v1/orders"
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an http url inside a template is reported`() {
        val code = """
            package com.example.shop

            fun endpoint(path: String) = "http://localhost:8080/${'$'}path"
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a url in a di package passes`() {
        val code = """
            package com.example.shop.di

            object ShopUrls {
                const val ORDERS = "https://api.example.com/v1/orders"
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a string without a url passes`() {
        val code = """
            package com.example.shop

            val scheme = "https"
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `custom allowed package segments are honored`() {
        val sut = HardcodedUrl(TestConfig("allowedPackageSegments" to listOf("config")))
        val code = """
            package com.example.config

            val orders = "https://api.example.com/v1/orders"
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an upper case url is reported`() {
        val code = """
            package com.example.shop

            val orders = "HTTPS://api.example.com/v1/orders"
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an xml namespace passes`() {
        val code = """
            package com.example.shop

            val android = "http://schemas.android.com/apk/res/android"
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a url after an xml namespace in one string is reported`() {
        val code = """
            package com.example.shop

            val text = "http://www.w3.org/2000/svg https://api.example.com"
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `custom allowed url prefixes are honored`() {
        val sut = HardcodedUrl(TestConfig("allowedUrlPrefixes" to listOf("https://example.com/")))
        val code = """
            package com.example.shop

            val help = "https://example.com/help"
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a loopback redirect url is reported by default`() {
        val code = """
            package com.example.auth

            fun redirectUri(port: Int) = "http://127.0.0.1:${'$'}port"
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an oauth scope is reported by default`() {
        val code = """
            package com.example.auth

            const val SCOPES = "https://www.googleapis.com/auth/drive.appdata openid email"
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }
}
