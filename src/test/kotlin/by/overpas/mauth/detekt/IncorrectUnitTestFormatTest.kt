package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class IncorrectUnitTestFormatTest {

    private val sut = IncorrectUnitTestFormat(Config.empty)

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

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an act assert body passes`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val actual = runCatching { decode() }

                    assertTrue(actual.isFailure)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an empty line inside a nested lambda does not separate blocks`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val actual = compute()

                    assertTrue {
                        val now = now()

                        actual > now
                    }
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a body wrapped in a table iteration passes`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    listOf(1 to 2, 2 to 3).forEach { (input, expected) ->
                        val actual = input + 1

                        assertEquals(expected, actual)
                    }
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an expression body wrapped in runTest passes`() {
        val code = """
            class T {
                @Test
                fun `a test`() = runTest {
                    val actual = load()

                    assertNull(actual)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a misplaced assertion is ignored`() {
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
    fun `a single block body is reported`() {
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

        assertEquals(1, findings.size)
    }

    @Test
    fun `an assertion wrapping the whole body is reported`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    assertFails {
                        decode()
                    }
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a four block body is reported`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    bringToFront()

                    pop()

                    val actual = active()

                    assertEquals(1, actual)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a function without the test annotation is ignored`() {
        val code = """
            class T {
                fun helper() {
                    val actual = compute()
                    assertEquals(2, actual)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `the assertion prefixes are configurable`() {
        val sut = IncorrectUnitTestFormat(TestConfig("assertionPrefixes" to listOf("expect")))
        val code = """
            class T {
                @Test
                fun `a test`() {
                    expectFails {
                        val actual = compute()

                        assertTrue(actual)
                    }
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `the test annotations are configurable`() {
        val sut = IncorrectUnitTestFormat(TestConfig("testAnnotations" to listOf("Scenario")))
        val code = """
            class T {
                @Scenario
                fun `a test`() {
                    val actual = compute()
                    assertEquals(2, actual)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }
}
