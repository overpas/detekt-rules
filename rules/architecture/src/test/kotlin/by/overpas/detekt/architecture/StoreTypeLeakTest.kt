package by.overpas.detekt.architecture

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class StoreTypeLeakTest {

    private val sut = StoreTypeLeak(Config.empty)

    @Test
    fun `a message import outside the store package is reported`() {
        val code = """
            package com.example.cart.totals

            import com.example.cart.checkout.CheckoutStore.Message

            class TotalsLoader(private val dispatch: (Message) -> Unit)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a qualified action outside the store package is reported once`() {
        val code = """
            package com.example.cart

            import com.example.cart.checkout.CheckoutStore

            class TotalsLoader(private val first: CheckoutStore.Action, private val second: CheckoutStore.Action)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a qualified message expression outside the store package is reported`() {
        val code = """
            package com.example.cart.totals

            import com.example.cart.checkout.CheckoutStore

            val loaded = CheckoutStore.Message.Loaded
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a store type in the store package passes`() {
        val code = """
            package com.example.cart.checkout

            class CheckoutExecutorFactory(private val dispatch: (CheckoutStore.Message) -> Unit)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a store type in a subpackage of the store package is reported by default`() {
        val code = """
            package com.example.cart.checkout.steps

            import com.example.cart.checkout.CheckoutStore.Message

            class PayStep(private val dispatch: (Message) -> Unit)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a store type in an allowed subpackage passes`() {
        val sut = StoreTypeLeak(TestConfig("allowedSubpackages" to listOf("steps")))
        val code = """
            package com.example.cart.checkout.steps.payment

            import com.example.cart.checkout.CheckoutStore.Action
            import com.example.cart.checkout.CheckoutStore.Message

            class PayStep(private val dispatch: (Message) -> Unit, private val forward: (Action) -> Unit)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an intent, a state and a label in the component package pass`() {
        val code = """
            package com.example.cart

            import com.example.cart.checkout.CheckoutStore

            class CartComponent(private val store: CheckoutStore) {
                fun onPayClick() = store.accept(CheckoutStore.Intent.OnPayClick)
                fun handle(label: CheckoutStore.Label, state: CheckoutStore.State) = Unit
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a message in the component package is reported`() {
        val code = """
            package com.example.cart

            import com.example.cart.checkout.CheckoutStore

            class CartComponent(private val message: CheckoutStore.Message)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a state in the ui package passes`() {
        val code = """
            package com.example.cart.ui

            import com.example.cart.checkout.CheckoutStore.State

            fun CartUi(state: State) = Unit
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an intent in the ui package is reported`() {
        val code = """
            package com.example.cart.ui

            import com.example.cart.checkout.CheckoutStore.Intent

            fun CartUi(onIntent: (Intent) -> Unit) = Unit
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a nested type of a class without the store suffix passes`() {
        val code = """
            package com.example.cart.ui

            import com.example.cart.checkout.CheckoutExecutor.Message

            fun CartUi(message: Message) = Unit
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `custom component types are honored`() {
        val sut = StoreTypeLeak(TestConfig("componentTypes" to listOf("State", "Label")))
        val code = """
            package com.example.cart

            import com.example.cart.checkout.CheckoutStore

            class CartComponent(private val intent: CheckoutStore.Intent)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }
}
