package by.overpas.detekt.architecture

import dev.detekt.api.Config
import dev.detekt.api.Finding
import dev.detekt.test.TestConfig
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals

class ProvidesInsteadOfInjectTest {

    private val environment = createEnvironment()

    private val sut = ProvidesInsteadOfInject(Config.empty)

    @Test
    fun `a provider that passes its parameters to a constructor is reported`() {
        val code = """
            annotation class Provides
            class Api
            class Repository(private val api: Api)
            object Providers {
                @Provides
                fun repository(api: Api): Repository = Repository(api)
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a provider that returns an interface proposes a binding`() {
        val code = """
            annotation class Provides
            class Api
            interface Repository
            class DefaultRepository(private val api: Api) : Repository
            object Providers {
                @Provides
                fun repository(api: Api): Repository {
                    return DefaultRepository(api = api)
                }
            }
        """.trimIndent()
        val expected = listOf(
            "`repository` only passes its parameters to `DefaultRepository`. " +
                "Put `@Inject` on `DefaultRepository` and bind it to `Repository` with `@Binds`.",
        )

        val messages = sut.lintWithContext(environment, code).map(Finding::message)

        assertEquals(expected, messages)
    }

    @Test
    fun `a provider without parameters that calls a constructor is reported`() {
        val code = """
            annotation class Provides
            class Clock
            object Providers {
                @Provides
                fun clock(): Clock = Clock()
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a provider that passes other arguments passes`() {
        val code = """
            annotation class Provides
            class Api(private val url: String)
            object Providers {
                @Provides
                fun api(): Api = Api("https://example.com")
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a provider that configures the instance passes`() {
        val code = """
            annotation class Provides
            class Api {
                fun configured(): Api = this
            }
            object Providers {
                @Provides
                fun api(): Api = Api().configured()
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a provider that calls a function passes`() {
        val code = """
            annotation class Provides
            class Api
            fun createApi(): Api = Api()
            object Providers {
                @Provides
                fun api(): Api = createApi()
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a provider of a library class passes`() {
        val code = """
            annotation class Provides
            object Providers {
                @Provides
                fun names(): ArrayList<String> = ArrayList()
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a function without a provider annotation passes`() {
        val code = """
            class Clock
            object Providers {
                fun clock(): Clock = Clock()
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `custom provider annotations are honored`() {
        val sut = ProvidesInsteadOfInject(TestConfig("providerAnnotations" to listOf("Creates")))
        val code = """
            annotation class Creates
            class Clock
            object Providers {
                @Creates
                fun clock(): Clock = Clock()
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }
}
