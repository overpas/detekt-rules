package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class MisplacedAssertionTest {

    private val rule = MisplacedAssertion(Config.empty)

    @Test
    fun `an arrange act assert body passes`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val input = 1

                    val actual = input + 1

                    assertEquals(2, actual)
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `several assertions in the last block pass`() {
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

        val findings = rule.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an assertion on a receiver counts as an assertion`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val page = open()

                    page.assertTitleShown()
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a last block without assertions is reported`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val input = 1

                    val actual = input + 1
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a last block that ends with a non assertion is reported`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    save()

                    val actual = read()
                    assertTrue(actual.isNotEmpty())
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an assertion outside the last block is reported`() {
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

        val findings = rule.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a single block body is ignored`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val actual = compute()
                    assertEquals(2, actual)
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a four block body is ignored`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val actual = compute()

                    assertNotNull(actual)

                    cleanUp()

                    assertEquals(2, actual)
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a function without the test annotation is ignored`() {
        val code = """
            class T {
                fun helper() {
                    val actual = compute()

                    val doubled = actual * 2
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `the assertion prefixes are configurable`() {
        val configured = MisplacedAssertion(TestConfig("assertionPrefixes" to listOf("expect")))
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val actual = compute()

                    expectThat(actual)
                }
            }
        """.trimIndent()

        val findings = configured.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `the test annotations are configurable`() {
        val configured = MisplacedAssertion(TestConfig("testAnnotations" to listOf("Scenario")))
        val code = """
            class T {
                @Scenario
                fun `a test`() {
                    val actual = compute()

                    val doubled = actual * 2
                }
            }
        """.trimIndent()

        val findings = configured.lint(code)

        assertEquals(1, findings.size)
    }
}
