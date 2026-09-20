package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class ExceptionMessageAssertionTest {

    private val rule = ExceptionMessageAssertion(Config.empty)

    @Test
    fun `a message read is reported`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val error = assertFails { decode() }
                    val named = error.message.orEmpty().contains("secret")

                    assertTrue(named)
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a localizedMessage read is reported`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val error = assertFails { decode() }

                    assertEquals("broken", error.localizedMessage)
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a stackTraceToString call is reported`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val error = assertFails { decode() }
                    val trace = error.stackTraceToString()

                    assertTrue(trace)
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `every read is reported`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val error = assertFails { decode() }
                    val named = error.message.orEmpty()
                    val trace = error.stackTraceToString()

                    assertEquals(named, trace)
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)

        assertEquals(2, findings.size)
    }

    @Test
    fun `a messages property is not reported`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val callbacks = record()

                    assertEquals(expected, callbacks.messages)
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an exception type assertion passes`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val generator = build()

                    assertFailsWith<IllegalStateException> {
                        generator.generate(config)
                    }
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a read outside a unit test is ignored`() {
        val code = """
            class T {
                fun helper(error: Throwable): String {
                    return error.message.orEmpty()
                }
            }
        """.trimIndent()

        val findings = rule.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `the accessors are configurable`() {
        val configured = ExceptionMessageAssertion(TestConfig("exceptionAccessors" to listOf("reason")))
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val error = assertFails { decode() }
                    val named = error.reason

                    assertTrue(named)
                }
            }
        """.trimIndent()

        val findings = configured.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `the test annotations are configurable`() {
        val configured = ExceptionMessageAssertion(TestConfig("testAnnotations" to listOf("Scenario")))
        val code = """
            class T {
                @Scenario
                fun `a test`() {
                    val error = assertFails { decode() }
                    val named = error.message

                    assertTrue(named)
                }
            }
        """.trimIndent()

        val findings = configured.lint(code)

        assertEquals(1, findings.size)
    }
}
