package by.overpas.detekt.architecture

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals

class LowCohesionTest {

    private val environment = createEnvironment()

    private val sut = LowCohesion(Config.empty)

    @Test
    fun `a class with two unrelated groups is reported`() {
        val code = """
            interface Cart { val prices: List<Int> }
            interface Analytics { fun log(event: String) }
            class OrderScreen(private val cart: Cart, private val analytics: Analytics) {
                fun total(): Int = cart.prices.sum()
                fun count(): Int = cart.prices.size
                fun track(event: String) = analytics.log(event)
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `groups that a function call links pass`() {
        val code = """
            interface Cart { val prices: List<Int> }
            interface Analytics { fun log(event: String) }
            class OrderScreen(private val cart: Cart, private val analytics: Analytics) {
                fun total(): Int = cart.prices.sum()
                fun count(): Int = cart.prices.size
                fun checkout() = track("total ${'$'}{total()}")
                private fun track(event: String) = analytics.log(event)
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `groups that a computed property links pass`() {
        val code = """
            class Form(private val name: String, private val email: String) {
                val isValid: Boolean get() = name.isNotBlank() && email.contains("@")
                fun greeting(): String = "Hello, ${'$'}name"
                fun initial(): Char = name.first()
                fun domain(): String = email.substringAfter("@")
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `groups that a property setter links pass`() {
        val code = """
            class Counter(private val listener: (Int) -> Unit) {
                private var count: Int = 0
                    set(value) {
                        field = value
                        listener(value)
                    }
                fun increment() { count += 1 }
                fun reset() { count = 0 }
                fun notifyZero() = listener(0)
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a class whose groups only toString links is reported`() {
        val code = """
            interface Cart { val prices: List<Int> }
            interface Analytics { fun log(event: String) }
            class OrderScreen(private val cart: Cart, private val analytics: Analytics) {
                fun total(): Int = cart.prices.sum()
                fun count(): Int = cart.prices.size
                fun track(event: String) = analytics.log(event)
                override fun toString(): String = "${'$'}cart ${'$'}analytics"
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a function that uses no member passes`() {
        val code = """
            interface Cart { val prices: List<Int> }
            class OrderScreen(private val cart: Cart) {
                fun total(): Int = cart.prices.sum()
                fun count(): Int = cart.prices.size
                fun format(value: Int): String = "${'$'}value USD"
                fun parse(value: String): Int = value.toInt()
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a local variable that shadows a property does not link the groups`() {
        val code = """
            class Labels(private val items: List<String>, private val name: String) {
                fun count(): Int = items.size
                fun first(): String = items.first()
                fun title(): String {
                    val items = name
                    return items
                }
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a companion function does not link the groups of its class`() {
        val code = """
            class Labels(private val items: List<String>, private val name: String) {
                fun count(): Int = format(items.size)
                fun first(): String = items.first()
                fun title(): String = format(name.length)

                companion object {
                    fun format(value: Int): String = "${'$'}value"
                }
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an object with two unrelated groups is reported`() {
        val code = """
            object Registry {
                private val users = mutableListOf<String>()
                private val orders = mutableListOf<Int>()
                fun addUser(user: String) { users += user }
                fun userCount(): Int = users.size
                fun addOrder(order: Int) { orders += order }
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a class with fewer functions than the minimum passes`() {
        val code = """
            interface Cart { val prices: List<Int> }
            interface Analytics { fun log(event: String) }
            class OrderScreen(private val cart: Cart, private val analytics: Analytics) {
                fun total(): Int = cart.prices.sum()
                fun track(event: String) = analytics.log(event)
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an interface passes`() {
        val code = """
            interface OrderScreen {
                val prices: List<Int>
                val events: MutableList<String>
                fun total(): Int = prices.sum()
                fun count(): Int = prices.size
                fun track(event: String) { events += event }
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a class with interface delegation passes`() {
        val code = """
            interface Cart { val prices: List<Int> }
            interface Analytics { fun log(event: String) }
            class OrderScreen(private val cart: Cart, analytics: Analytics) : Analytics by analytics {
                fun total(): Int = cart.prices.sum()
                fun count(): Int = cart.prices.size
                fun track(event: String) = log(event)
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a data class passes`() {
        val code = """
            data class Order(val prices: List<Int>, val events: List<String>) {
                fun total(): Int = prices.sum()
                fun count(): Int = prices.size
                fun lastEvent(): String = events.last()
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a data class is reported when data classes are not ignored`() {
        val sut = LowCohesion(TestConfig("areDataClassesIgnored" to false))
        val code = """
            data class Order(val prices: List<Int>, val events: List<String>) {
                fun total(): Int = prices.sum()
                fun count(): Int = prices.size
                fun lastEvent(): String = events.last()
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `custom allowed components are honored`() {
        val sut = LowCohesion(TestConfig("allowedComponents" to 2))
        val code = """
            interface Cart { val prices: List<Int> }
            interface Analytics { fun log(event: String) }
            class OrderScreen(private val cart: Cart, private val analytics: Analytics) {
                fun total(): Int = cart.prices.sum()
                fun count(): Int = cart.prices.size
                fun track(event: String) = analytics.log(event)
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a custom minimum of functions is honored`() {
        val sut = LowCohesion(TestConfig("minFunctions" to 2))
        val code = """
            interface Cart { val prices: List<Int> }
            interface Analytics { fun log(event: String) }
            class OrderScreen(private val cart: Cart, private val analytics: Analytics) {
                fun total(): Int = cart.prices.sum()
                fun track(event: String) = analytics.log(event)
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `custom ignored functions are honored`() {
        val sut = LowCohesion(TestConfig("ignoredFunctions" to emptyList<String>()))
        val code = """
            interface Cart { val prices: List<Int> }
            interface Analytics { fun log(event: String) }
            class OrderScreen(private val cart: Cart, private val analytics: Analytics) {
                fun total(): Int = cart.prices.sum()
                fun count(): Int = cart.prices.size
                fun track(event: String) = analytics.log(event)
                override fun toString(): String = "${'$'}cart ${'$'}analytics"
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }
}
