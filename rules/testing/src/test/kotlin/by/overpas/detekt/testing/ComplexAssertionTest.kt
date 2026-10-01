package by.overpas.detekt.testing

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class ComplexAssertionTest {

    private val sut = ComplexAssertion(Config.empty)

    @Test
    fun `an assertion of variables passes`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val actual = compute()

                    assertEquals(expected, actual)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a literal assertion passes`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val actual = compute()

                    assertEquals("abc", actual)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a call in the actual argument is reported`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val a = create()

                    assertEquals(expected, a.b())
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an object creation in the assertion is reported`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val expected = 1

                    assertEquals(expected, Actual())
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a call in the expected argument is reported`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val actual = compute()

                    assertEquals(a.getExpected(), actual)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an extension call in the assertion is reported`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val expected = 1

                    assertEquals(expected, "".toActual())
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an object creation with literal arguments is reported`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val input = sanitize("5")

                    assertEquals(CounterInput.Accepted(5L), input)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a call in the assertion receiver is reported`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    prepare()

                    open().assertTitleShown()
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a variable receiver passes`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val page = open()

                    page.assertTitleShown()
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a property access passes`() {
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
    fun `a qualified reference passes`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val input = sanitize("a")

                    assertEquals(CounterInput.Discarded, input)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a callable reference passes`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val actual = mapper()

                    assertEquals(expected, OtpInfo::id)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an operator over variables passes`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val time2 = clock.now

                    assertTrue(time2 > time1)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a lambda argument passes`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val generator = create()

                    assertFailsWith<IllegalStateException> {
                        generator.generate(config)
                    }
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a call in an assertion lambda passes`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val actual = compute()

                    assertTrue { actual > now() }
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a chained assertion passes`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val page = open()

                    page.assertTitleShown().assertBodyShown()
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `two calls in one assertion are reported twice`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    prepare()

                    assertEquals(expected(), actual())
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(2, findings.size)
    }

    @Test
    fun `a chain of calls is reported once`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val a = create()

                    assertEquals(expected, a.b().c())
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a call in a string template is reported`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val actual = compute()

                    assertEquals(expected, actual, "at ${'$'}{now()}")
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an assertion inside a table iteration is reported`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    listOf(1 to 2).forEach { (input, expected) ->
                        val actual = input

                        assertEquals(expected, compute(actual))
                    }
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
                    val expected = 1

                    assertEquals(expected, compute())
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `the assertion prefixes are configurable`() {
        val sut = ComplexAssertion(TestConfig("assertionPrefixes" to listOf("expect")))
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val actual = compute()

                    expectThat(transform(actual))
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `the test annotations are configurable`() {
        val sut = ComplexAssertion(TestConfig("testAnnotations" to listOf("Scenario")))
        val code = """
            class T {
                @Scenario
                fun `a test`() {
                    val expected = 1

                    assertEquals(expected, compute())
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }
}
