package by.overpas.detekt.architecture

import dev.detekt.api.Config
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals

class HiddenAbstractionTest {

    private val environment = createEnvironment()

    private val sut = HiddenAbstraction(Config.empty)

    @Test
    fun `more helper functions than entry points in a class are reported once`() {
        val code = """
            class Checkout(private val price: Int) {
                fun pay(): Int = fee() + tax()
                private fun fee(): Int = rate()
                private fun tax(): Int = rate()
                private fun rate(): Int = price / 10
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `as many helper functions as entry points pass`() {
        val code = """
            class Checkout(private val price: Int) {
                fun pay(): Int = price + fee()
                fun refund(): Int = price - tax()
                private fun fee(): Int = price / 10
                private fun tax(): Int = price / 5
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a public helper function still counts as a helper`() {
        val code = """
            class Checkout(private val price: Int) {
                fun pay(): Int = fee() + tax()
                fun fee(): Int = price / 10
                fun tax(): Int = price / 5
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an override function counts as an entry point also when the class calls it`() {
        val code = """
            interface Payment {
                fun pay(): Int
                fun refund(): Int
            }
            class Checkout(private val price: Int) : Payment {
                override fun pay(): Int = fee() + tax() - refund()
                override fun refund(): Int = price
                private fun fee(): Int = price / 10
                private fun tax(): Int = price / 5
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `abstract functions count as entry points`() {
        val code = """
            abstract class Page {
                abstract fun title(): String
                abstract fun body(): String
                fun render(): String = header() + footer()
                private fun header(): String = title()
                private fun footer(): String = body()
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an uncalled private function is not a helper`() {
        val code = """
            class Checkout(private val price: Int) {
                fun pay(): Int = fee()
                private fun fee(): Int = price / 10
                private fun unused(): Int = price
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an uncalled private function is not an entry point`() {
        val code = """
            class Checkout(private val price: Int) {
                fun pay(): Int = fee() + tax()
                private fun fee(): Int = price / 10
                private fun tax(): Int = price / 5
                private fun unused(): Int = price
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `recursion alone does not make a helper`() {
        val code = """
            class Node(val child: Node?)
            class Tree {
                fun depth(node: Node?): Int = if (node == null) 0 else 1 + depth(node.child)
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `local functions count as helpers`() {
        val code = """
            class Checkout(private val price: Int) {
                fun pay(): Int {
                    fun fee(): Int = price / 10
                    fun tax(): Int = price / 5
                    return fee() + tax()
                }
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `companion functions count toward the class`() {
        val code = """
            class Checkout(private val price: Int) {
                fun pay(): Int = price + fee(price)
                companion object {
                    fun fee(price: Int): Int = price / rate()
                    private fun rate(): Int = 10
                }
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a companion object is not checked on its own`() {
        val code = """
            class Checkout(private val price: Int) {
                fun pay(): Int = price + fee(price)
                fun refund(): Int = price
                fun cancel(): Int = 0
                companion object {
                    fun fee(price: Int): Int = price / rate() + base()
                    private fun rate(): Int = 10
                    private fun base(): Int = 1
                }
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a nested class is its own scope`() {
        val code = """
            class Outer {
                fun run(): Int = Inner().start()
                class Inner {
                    fun start(): Int = first() + second()
                    private fun first(): Int = 1
                    private fun second(): Int = 2
                }
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `calls from a nested class count as calls inside the outer class`() {
        val code = """
            class Outer {
                fun run(): Int = 0
                fun first(): Int = 1
                fun second(): Int = 2
                inner class Inner {
                    fun start(): Int = first() + second()
                }
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a callable reference counts as a call`() {
        val code = """
            class Checkout(private val amounts: List<Int>) {
                fun pay(): Int = amounts.map(::fee).sum() + amounts.map(::tax).sum()
                private fun fee(amount: Int): Int = amount / 10
                private fun tax(amount: Int): Int = amount / 5
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an operator call counts as a call`() {
        val code = """
            class Money(val cents: Int)
            class Ledger {
                fun balance(income: Money, costs: Money): Money = income + costs - costs
                private operator fun Money.plus(other: Money): Money = Money(cents + other.cents)
                private operator fun Money.minus(other: Money): Money = Money(cents - other.cents)
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an infix call counts as a call`() {
        val code = """
            class Money(val cents: Int)
            class Ledger {
                fun balance(income: Money, costs: Money): Money = income merge costs split costs
                private infix fun Money.merge(other: Money): Money = Money(cents + other.cents)
                private infix fun Money.split(other: Money): Money = Money(cents - other.cents)
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `more top-level helper functions than entry points are reported`() {
        val code = """
            fun pay(price: Int): Int = fee(price) + tax(price)
            private fun fee(price: Int): Int = price / 10
            private fun tax(price: Int): Int = price / 5
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a balanced file passes`() {
        val code = """
            fun pay(price: Int): Int = price + fee(price)
            fun refund(price: Int): Int = price
            private fun fee(price: Int): Int = price / 10
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `top-level helper functions that a class calls are reported on the file`() {
        val code = """
            class Checkout(private val price: Int) {
                fun pay(): Int = fee(price) + tax(price)
            }
            private fun fee(price: Int): Int = price / 10
            private fun tax(price: Int): Int = price / 5
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an interface is checked`() {
        val code = """
            interface Formatter {
                fun format(value: Int): String = prefix() + value + suffix()
                fun prefix(): String = "["
                fun suffix(): String = "]"
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an object is checked`() {
        val code = """
            object Formatter {
                fun format(value: Int): String = prefix() + value + suffix()
                private fun prefix(): String = "["
                private fun suffix(): String = "]"
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an object expression is its own scope`() {
        val code = """
            interface Task {
                fun run(): Int
            }
            fun task(): Task =
                object : Task {
                    override fun run(): Int = first() + second()
                    private fun first(): Int = 1
                    private fun second(): Int = 2
                }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }
}
