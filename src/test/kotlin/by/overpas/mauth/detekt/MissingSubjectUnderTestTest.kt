package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class MissingSubjectUnderTestTest {

    private val sut = MissingSubjectUnderTest(Config.empty)

    @Test
    fun `a sut call in the act block passes`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val input = 1

                    val actual = sut(input)

                    assertEquals(2, actual)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a sut extension call in the act block passes`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val sut = Long::asByteArray

                    val actual = 1L.sut()

                    assertEquals(1, actual.size)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a sut call in a nested lambda passes`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val actual = runCatching { "x".sut() }

                    assertTrue(actual.isFailure)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a sut property access passes`() {
        val code = """
            class T {
                private val sut = Store()

                @Test
                fun `a test`() {
                    val actual = sut.state.value

                    assertEquals(0, actual)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a sut usage as the whole statement passes`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val actual = mutableListOf<Int>()

                    sut

                    assertEquals(0, actual.size)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a sut usage in a runTest body passes`() {
        val code = """
            class T {
                @Test
                fun `a test`() = runTest {
                    val input = 1

                    val actual = sut.compute(input)

                    assertEquals(2, actual)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a sut usage in a forEach body passes`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    listOf(1 to 2).forEach { (input, expected) ->
                        val sut = Int::inc

                        val actual = input.sut()

                        assertEquals(expected, actual)
                    }
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an act block without sut is reported`() {
        val code = """
            class T {
                private val validator = Validator()

                @Test
                fun `a test`() {
                    val input = 1

                    val actual = validator.validate(input)

                    assertEquals(2, actual)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `sut only in the arrange block is reported`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val sut = Validator()

                    val actual = compute()

                    assertEquals(2, actual)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `sut only in the assert block is reported`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val actual = compute()

                    assertEquals(sut.value, actual)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a similar name is reported`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val actual = sutFactory.create()

                    assertEquals(2, actual)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a function without a test annotation is ignored`() {
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

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a body with more than three blocks is ignored`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val a = 1

                    val b = 2

                    val actual = a + b

                    assertEquals(3, actual)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a configured subject name is respected`() {
        val sut = MissingSubjectUnderTest(TestConfig("subjectName" to "subject"))
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val actual = subject.compute()

                    assertEquals(2, actual)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }
}
