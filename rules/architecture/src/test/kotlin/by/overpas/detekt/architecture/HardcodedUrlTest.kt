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
    fun `custom allowed packages are honored`() {
        val sut = HardcodedUrl(TestConfig("allowedPackages" to listOf("config")))
        val code = """
            package com.example.config

            val orders = "https://api.example.com/v1/orders"
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }
}
