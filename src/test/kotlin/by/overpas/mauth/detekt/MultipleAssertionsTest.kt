package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class MultipleAssertionsTest {

    private val sut = MultipleAssertions(Config.empty)

    @Test
    fun `one assertion in the last block passes`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val actual = compute()

                    assertEquals(2, actual)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `two assertions in the last block are reported`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val actual = compute()

                    assertNotNull(actual)
                    assertEquals(2, actual)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an assertOn block counts as one assertion`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val actual = compute()

                    assertOn(actual) {
                        assertEquals(1, first)
                        assertEquals(2, second)
                    }
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an assertOn block with another assertion is reported`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val actual = compute()

                    assertOn(actual) {
                        assertEquals(1, first)
                    }
                    assertNotNull(actual)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a single block body is ignored`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val actual = compute()
                    assertNotNull(actual)
                    assertEquals(2, actual)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a four block body is ignored`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val actual = compute()

                    cleanUp()

                    prepare()

                    assertNotNull(actual)
                    assertEquals(2, actual)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a function without the test annotation is ignored`() {
        val code = """
            class T {
                fun helper() {
                    val actual = compute()

                    assertNotNull(actual)
                    assertEquals(2, actual)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `the assertion prefixes are configurable`() {
        val sut = MultipleAssertions(TestConfig("assertionPrefixes" to listOf("expect")))
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val actual = compute()

                    expectThat(actual)
                    expectThat(actual)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `the test annotations are configurable`() {
        val sut = MultipleAssertions(TestConfig("testAnnotations" to listOf("Scenario")))
        val code = """
            class T {
                @Scenario
                fun `a test`() {
                    val actual = compute()

                    assertNotNull(actual)
                    assertEquals(2, actual)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }
}
