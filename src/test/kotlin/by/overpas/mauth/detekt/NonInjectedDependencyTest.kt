package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class NonInjectedDependencyTest {

    private val sut = NonInjectedDependency(Config.empty)

    @Test
    fun `a provider that constructs the provided dependency passes`() {
        val code = """
            object Providers {
                @Provides
                fun repository(api: Api): Repository =
                    DefaultRepository(api = api)
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a constructor call in an argument is reported`() {
        val code = """
            object Providers {
                @Provides
                fun repository(): Repository =
                    DefaultRepository(api = HttpApi())
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a constructor call in a receiver is reported`() {
        val code = """
            object Providers {
                @Provides
                fun queries(driver: SqlDriver): Queries =
                    Database(driver).queries
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a constructor call in a local variable is reported`() {
        val code = """
            object Providers {
                @Provides
                fun repository(): Repository {
                    val api = HttpApi()
                    return DefaultRepository(api = api)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a constructor call in a provider property is reported`() {
        val code = """
            interface Graph {
                @Provides
                val repository: Repository
                    get() = DefaultRepository(api = HttpApi())
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a constructor call in the lambda of the provided dependency passes`() {
        val code = """
            object Providers {
                @Provides
                fun childBuilder(): ChildBuilder =
                    ChildBuilder { context ->
                        Child(context = context)
                    }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a function call on an injected dependency passes`() {
        val code = """
            object Providers {
                @Provides
                fun driver(factory: DriverFactory): SqlDriver =
                    factory.create()
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a function without a provider annotation passes`() {
        val code = """
            fun repository(): Repository =
                DefaultRepository(api = HttpApi())
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `custom provider annotations are honored`() {
        val sut = NonInjectedDependency(TestConfig("providerAnnotations" to listOf("Factory")))
        val code = """
            object Providers {
                @Factory
                fun repository(): Repository =
                    DefaultRepository(api = HttpApi())
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }
}
