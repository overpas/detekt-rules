package by.overpas.detekt.architecture

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals

class ConcreteCollaboratorTest {

    private val environment = createEnvironment()

    private val sut = ConcreteCollaborator(Config.empty)

    @Test
    fun `a parameter of a class that implements a project interface is reported`() {
        val code = """
            interface PaymentGateway
            class CardPaymentGateway : PaymentGateway
            class Checkout(private val gateway: CardPaymentGateway)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a parameter of a class that inherits a project interface from its superclass is reported`() {
        val code = """
            interface PaymentGateway
            abstract class BasePaymentGateway : PaymentGateway
            class CardPaymentGateway : BasePaymentGateway()
            class Checkout(private val gateway: CardPaymentGateway?)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a parameter of an interface passes`() {
        val code = """
            interface PaymentGateway
            class Checkout(private val gateway: PaymentGateway)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a parameter of a class without a project interface passes`() {
        val code = """
            class Clock
            class Checkout(private val clock: Clock, private val names: ArrayList<String>)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a parameter of a data class passes`() {
        val code = """
            interface Discount
            data class Coupon(val code: String) : Discount
            class Checkout(private val coupon: Coupon)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a data class constructor passes`() {
        val code = """
            interface PaymentGateway
            class CardPaymentGateway : PaymentGateway
            data class Gateways(val gateway: CardPaymentGateway)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a data class constructor is reported when data classes are not ignored`() {
        val sut = ConcreteCollaborator(TestConfig("areDataClassesIgnored" to false))
        val code = """
            interface PaymentGateway
            class CardPaymentGateway : PaymentGateway
            data class Gateways(val gateway: CardPaymentGateway)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a secondary constructor is reported`() {
        val code = """
            interface PaymentGateway
            class CardPaymentGateway : PaymentGateway
            class Checkout {
                constructor(gateway: CardPaymentGateway)
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a compiled interface from the root package of the file is a project interface`() {
        val code = """
            package java.util.names

            class Names(private val names: java.util.ArrayList<String>)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a compiled interface outside the root package of the file passes`() {
        val sut = ConcreteCollaborator(TestConfig("rootPackageDepth" to 3))
        val code = """
            package java.util.names

            class Names(private val names: java.util.ArrayList<String>)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }
}
