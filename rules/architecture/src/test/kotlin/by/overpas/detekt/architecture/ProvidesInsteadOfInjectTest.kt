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
            class OrderApi
            class OrderRepository(private val api: OrderApi)
            object Providers {
                @Provides
                fun repository(api: OrderApi): OrderRepository = OrderRepository(api)
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a provider that returns an interface proposes a binding`() {
        val code = """
            annotation class Provides
            class OrderApi
            interface OrderRepository
            class HttpOrderRepository(private val api: OrderApi) : OrderRepository
            object Providers {
                @Provides
                fun repository(api: OrderApi): OrderRepository {
                    return HttpOrderRepository(api = api)
                }
            }
        """.trimIndent()
        val expected = listOf(
            "`repository` only passes its parameters to `HttpOrderRepository`. " +
                "Put `@Inject` on `HttpOrderRepository` and bind it to `OrderRepository` with `@Binds`.",
        )

        val messages = sut.lintWithContext(environment, code).map(Finding::message)

        assertEquals(expected, messages)
    }

    @Test
    fun `a provider without parameters that calls a constructor is reported`() {
        val code = """
            annotation class Provides
            class PriceFormatter
            object Providers {
                @Provides
                fun priceFormatter(): PriceFormatter = PriceFormatter()
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a provider that passes other arguments passes`() {
        val code = """
            annotation class Provides
            class OrderApi(private val path: String)
            object Providers {
                @Provides
                fun api(): OrderApi = OrderApi("orders")
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a provider that configures the instance passes`() {
        val code = """
            annotation class Provides
            class OrderApi {
                fun configured(): OrderApi = this
            }
            object Providers {
                @Provides
                fun api(): OrderApi = OrderApi().configured()
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a provider that calls a function passes`() {
        val code = """
            annotation class Provides
            class OrderApi
            fun createOrderApi(): OrderApi = OrderApi()
            object Providers {
                @Provides
                fun api(): OrderApi = createOrderApi()
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
            class PriceFormatter
            object Providers {
                fun priceFormatter(): PriceFormatter = PriceFormatter()
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
            class PriceFormatter
            object Providers {
                @Creates
                fun priceFormatter(): PriceFormatter = PriceFormatter()
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }
}
