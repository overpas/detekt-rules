package by.overpas.detekt.style

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals

class CollaboratorNameMismatchTest {

    private val environment = createEnvironment()

    private val sut = CollaboratorNameMismatch(Config.empty)

    @Test
    fun `a name without the last word of its type is reported`() {
        val code = """
            interface PaymentGateway
            class Checkout(private val payments: PaymentGateway)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a name after the first word of its type is reported`() {
        val code = """
            interface InvoiceArchiveRepository
            class Billing(private val invoices: InvoiceArchiveRepository)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a name with the last word of its type passes`() {
        val code = """
            interface PaymentGateway
            interface PriceDb
            class Checkout(private val gateway: PaymentGateway, private val cachedPriceDb: PriceDb?)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a name with an acronym of its type passes`() {
        val code = """
            interface SSOProvider
            class SignIn(private val ssoProvider: SSOProvider)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `primitive, stdlib and function types pass`() {
        val code = """
            class RetryPolicy(
                private val retries: Int,
                private val names: List<String>,
                private val onChange: () -> Unit,
                private val onClick: (() -> Unit)?,
            )
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a type parameter passes`() {
        val code = """
            class Box<T>(private val content: T)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a constructor parameter that is not a property passes`() {
        val code = """
            interface PaymentGateway
            class Checkout(payments: PaymentGateway)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a data class passes`() {
        val code = """
            class EmailAddress
            data class Contact(val primary: EmailAddress)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a data class is reported when data classes are not ignored`() {
        val sut = CollaboratorNameMismatch(TestConfig("areDataClassesIgnored" to false))
        val code = """
            class EmailAddress
            data class Contact(val primary: EmailAddress)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a type from a package that is not excluded is reported`() {
        val sut = CollaboratorNameMismatch(TestConfig("excludedPackages" to emptyList<String>()))
        val code = """
            class RetryPolicy(private val retries: Int)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a name without the last word of a non-ascii type is reported`() {
        val code = """
            class Ä
            class Mood(private val mood: Ä)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a dagger lazy under any name passes`() {
        val dagger = """
            package dagger

            interface Lazy<T> {
                fun get(): T
            }
        """.trimIndent()
        val code = """
            import dagger.Lazy

            interface PaymentGateway
            class Checkout(private val gateway: Lazy<PaymentGateway>)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code, dagger)

        assertEquals(0, findings.size)
    }
}
