package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class HelperFunctionInTestTest {

    private val sut = HelperFunctionInTest(Config.empty)

    @Test
    fun `a test class with lifecycle functions and properties passes`() {
        val code = """
            class T {
                private val store = Store()

                @BeforeTest
                fun setUp() {
                    store.init()
                }

                @Test
                fun `a test`() {
                    val actual = store.compute()

                    assertEquals(2, actual)
                }

                @AfterTest
                fun tearDown() {
                    store.dispose()
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a fake class in the test file passes`() {
        val code = """
            private class FakeCallbacks : Callbacks {
                override fun onMessage(message: String) {
                }
            }

            class T {
                @Test
                fun `a test`() {
                    val actual = FakeCallbacks()

                    assertNotNull(actual)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a graph interface in the test file passes`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val actual = createGraph<TGraph>()

                    assertNotNull(actual)
                }
            }

            @DependencyGraph(AppScope::class)
            interface TGraph {
                fun inject(test: T)
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a nested binding container in the test class passes`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val actual = FakeBindings()

                    assertNotNull(actual)
                }

                @BindingContainer
                class FakeBindings {
                    @Provides
                    fun value(): Int = 1
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an object literal in a test passes`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val actual = object : Callbacks {
                        override fun onMessage(message: String) {
                        }
                    }

                    assertNotNull(actual)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a file without tests passes`() {
        val code = """
            class LoginPage {
                fun login() {
                    click()
                }

                private fun click() {
                }
            }

            fun loginPage() = LoginPage()
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a helper function in the test class is reported`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val actual = compute()

                    assertEquals(2, actual)
                }

                private fun compute(): Int = 2
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a top-level helper function in the test file is reported`() {
        val code = """
            private fun compute(): Int = 2

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
    fun `a helper function in the companion object is reported`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val actual = compute()

                    assertEquals(2, actual)
                }

                companion object {
                    @BeforeClass
                    fun setUpClass() {
                    }

                    fun compute(): Int = 2
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a local function in a test is reported`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    fun compute(): Int = 2

                    val actual = compute()

                    assertEquals(2, actual)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an anonymous function in a test passes`() {
        val code = """
            class T {
                @Test
                fun `a test`() {
                    val actual = listOf(1).map(fun(it: Int) = it * 2)

                    assertEquals(listOf(2), actual)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `the lifecycle annotations are configurable`() {
        val sut = HelperFunctionInTest(TestConfig("lifecycleAnnotations" to listOf("Test")))
        val code = """
            class T {
                @BeforeTest
                fun setUp() {
                }

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
    fun `the test annotations are configurable`() {
        val sut = HelperFunctionInTest(
            TestConfig("testAnnotations" to listOf("Scenario"), "lifecycleAnnotations" to listOf("Scenario")),
        )
        val code = """
            class T {
                @Scenario
                fun `a test`() {
                    val actual = compute()

                    assertEquals(2, actual)
                }

                private fun compute(): Int = 2
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }
}
