package by.overpas.detekt.architecture

import dev.detekt.api.Finding
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import dev.detekt.test.utils.compileForTest
import kotlin.io.path.createParentDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class WeightedComplexityTest {

    @Test
    fun `each flat call adds one to the function`() {
        val sut = WeightedComplexity(TestConfig("classLimit" to 0))
        val code = """
            class Greeter {
                fun greet() {
                    println("a")
                    println("b")
                }
            }
        """.trimIndent()
        val expected = listOf(
            "`Greeter` has a weighted complexity of 3.0 (limit 0.0, normal bucket). Largest parts: `greet()` 3.0.",
        )

        val messages = sut.lint(code).map(Finding::message)

        assertEquals(expected, messages)
    }

    @Test
    fun `calls in the bodies of control structures are one level deeper`() {
        val sut = WeightedComplexity(TestConfig("classLimit" to 0))
        val code = """
            class Printer {
                fun print(count: Int) {
                    if (count > 0) println("if") else println("else")
                    when (count) {
                        0 -> println("zero")
                        else -> println("other")
                    }
                    for (index in 0..count) println("for")
                    while (count > 1) println("while")
                    do println("do") while (count > 2)
                    try {
                        println("try")
                    } catch (e: Exception) {
                        println("catch")
                    } finally {
                        println("finally")
                    }
                }
            }
        """.trimIndent()
        val expected = listOf(
            "`Printer` has a weighted complexity of 17.5 (limit 0.0, normal bucket). Largest parts: `print()` 17.5.",
        )

        val messages = sut.lint(code).map(Finding::message)

        assertEquals(expected, messages)
    }

    @Test
    fun `calls in a lambda are one level deeper`() {
        val sut = WeightedComplexity(TestConfig("classLimit" to 0))
        val code = """
            class Printer {
                fun print(items: List<String>) {
                    items.forEach { item ->
                        println(item)
                        item.let { println(it) }
                    }
                }
            }
        """.trimIndent()
        val expected = listOf(
            "`Printer` has a weighted complexity of 6.0 (limit 0.0, normal bucket). Largest parts: `print()` 6.0.",
        )

        val messages = sut.lint(code).map(Finding::message)

        assertEquals(expected, messages)
    }

    @Test
    fun `else if stays on the level of the first if`() {
        val sut = WeightedComplexity(TestConfig("classLimit" to 0))
        val code = """
            class Sign {
                fun print(value: Int) {
                    if (value > 0) {
                        println("positive")
                    } else if (value < 0) {
                        println("negative")
                    } else {
                        println("zero")
                        println("none")
                    }
                }
            }
        """.trimIndent()
        val expected = listOf(
            "`Sign` has a weighted complexity of 8.0 (limit 0.0, normal bucket). Largest parts: `print()` 8.0.",
        )

        val messages = sut.lint(code).map(Finding::message)

        assertEquals(expected, messages)
    }

    @Test
    fun `each earlier call in a chain adds a tenth`() {
        val sut = WeightedComplexity(TestConfig("classLimit" to 0))
        val code = """
            class Names {
                fun initials(names: List<String>): String =
                    names.map { it.first() }.filter { it.isUpperCase() }.joinToString("")

                fun size(names: List<String>): Int =
                    names.first().length.plus(1)
            }
        """.trimIndent()
        val expected = listOf(
            "`Names` has a weighted complexity of 9.9 (limit 0.0, normal bucket). " +
                "Largest parts: `initials()` 6.8, `size()` 3.1.",
        )

        val messages = sut.lint(code).map(Finding::message)

        assertEquals(expected, messages)
    }

    @Test
    fun `many parameters multiply the function by the square root of their count`() {
        val sut = WeightedComplexity(TestConfig("classLimit" to 0))
        val code = """
            class Box {
                fun volume(width: Int, height: Int, depth: Int, scale: Int): Int = width * height
            }
        """.trimIndent()
        val expected = listOf(
            "`Box` has a weighted complexity of 4.0 (limit 0.0, normal bucket). Largest parts: `volume()` 4.0.",
        )

        val messages = sut.lint(code).map(Finding::message)

        assertEquals(expected, messages)
    }

    @Test
    fun `a function without calls costs one`() {
        val sut = WeightedComplexity(TestConfig("classLimit" to 0))
        val code = """
            class Empty {
                fun nothing() {}
                fun one(value: Int) {}
            }
        """.trimIndent()
        val expected = listOf(
            "`Empty` has a weighted complexity of 2.0 (limit 0.0, normal bucket). " +
                "Largest parts: `nothing()` 1.0, `one()` 1.0.",
        )

        val messages = sut.lint(code).map(Finding::message)

        assertEquals(expected, messages)
    }

    @Test
    fun `a property costs the nodes of its type`() {
        val sut = WeightedComplexity(TestConfig("classLimit" to 0))
        val code = """
            class Store(val name: String, val tags: Set<String>) {
                val index: Map<String, List<Int>> = emptyMap()
                val anything: List<*>? = null
                val inferred = 0
            }
        """.trimIndent()
        val expected = listOf(
            "`Store` has a weighted complexity of 11.0 (limit 0.0, normal bucket). " +
                "Largest parts: `index` 5.0, `tags` 2.0, `anything` 2.0.",
        )

        val messages = sut.lint(code).map(Finding::message)

        assertEquals(expected, messages)
    }

    @Test
    fun `a function type costs one plus the nodes of its receiver, parameters and return type`() {
        val sut = WeightedComplexity(TestConfig("classLimit" to 0))
        val code = """
            class Handlers {
                val onClick: (Int, String) -> Unit = { _, _ -> }
                val onLoad: suspend String.() -> Int? = { 0 }
            }
        """.trimIndent()
        val expected = listOf(
            "`Handlers` has a weighted complexity of 7.0 (limit 0.0, normal bucket). " +
                "Largest parts: `onClick` 4.0, `onLoad` 3.0.",
        )

        val messages = sut.lint(code).map(Finding::message)

        assertEquals(expected, messages)
    }

    @Test
    fun `calls in an initializer, accessors and a delegate add to the property`() {
        val sut = WeightedComplexity(TestConfig("classLimit" to 0))
        val code = """
            class Counter {
                private val values = mutableListOf<Int>()
                val total: Int
                    get() = values.sum()
                var last: Int = 0
                    set(value) {
                        field = value
                        values.add(value)
                    }
                val doubled by lazy { values.sum() * 2 }
            }
        """.trimIndent()
        val expected = listOf(
            "`Counter` has a weighted complexity of 10.5 (limit 0.0, normal bucket). " +
                "Largest parts: `doubled` 4.5, `values` 2.0, `total` 2.0.",
        )

        val messages = sut.lint(code).map(Finding::message)

        assertEquals(expected, messages)
    }

    @Test
    fun `an abstract property costs one`() {
        val sut = WeightedComplexity(TestConfig("classLimit" to 0))
        val code = """
            interface Source {
                val items: Map<String, Int>
                val count: Int get() = items.size
                fun load(key: String)
            }

            abstract class Base {
                abstract val names: List<String>
            }
        """.trimIndent()
        val expected = listOf(
            "`Source` has a weighted complexity of 3.0 (limit 0.0, normal bucket). " +
                "Largest parts: `items` 1.0, `count` 1.0, `load()` 1.0.",
            "`Base` has a weighted complexity of 1.0 (limit 0.0, normal bucket). Largest parts: `names` 1.0.",
        )

        val messages = sut.lint(code).map(Finding::message)

        assertEquals(expected, messages)
    }

    @Test
    fun `a nested class is scored on its own and adds one to its parent`() {
        val sut = WeightedComplexity(TestConfig("classLimit" to 0))
        val code = """
            class Outer {
                fun run() {}

                class Nested {
                    fun print() {
                        println("a")
                    }
                }

                companion object {
                    fun create(): Outer = Outer()
                }
            }
        """.trimIndent()
        val expected = listOf(
            "`Nested` has a weighted complexity of 2.0 (limit 0.0, normal bucket). Largest parts: `print()` 2.0.",
            "`Companion` has a weighted complexity of 2.0 (limit 0.0, normal bucket). " +
                "Largest parts: `create()` 2.0.",
            "`Outer` has a weighted complexity of 3.0 (limit 0.0, normal bucket). " +
                "Largest parts: `run()` 1.0, `Nested` 1.0, `Companion` 1.0.",
        )

        val messages = sut.lint(code).map(Finding::message)

        assertEquals(expected, messages)
    }

    @Test
    fun `an inner class adds its full score to its parent`() {
        val sut = WeightedComplexity(TestConfig("classLimit" to 0))
        val code = """
            class Outer {
                fun run() {}

                inner class Inner {
                    fun print() {
                        println("a")
                    }
                }
            }
        """.trimIndent()
        val expected = listOf(
            "`Inner` has a weighted complexity of 2.0 (limit 0.0, normal bucket). Largest parts: `print()` 2.0.",
            "`Outer` has a weighted complexity of 3.0 (limit 0.0, normal bucket). " +
                "Largest parts: `Inner` 2.0, `run()` 1.0.",
        )

        val messages = sut.lint(code).map(Finding::message)

        assertEquals(expected, messages)
    }

    @Test
    fun `an object literal adds its calls to the enclosing function one level deeper`() {
        val sut = WeightedComplexity(TestConfig("classLimit" to 0))
        val code = """
            class Factory {
                fun create(): Runnable =
                    object : Runnable {
                        override fun run() {
                            println("a")
                            println("b")
                        }
                    }
            }
        """.trimIndent()
        val expected = listOf(
            "`Factory` has a weighted complexity of 3.5 (limit 0.0, normal bucket). Largest parts: `create()` 3.5.",
        )

        val messages = sut.lint(code).map(Finding::message)

        assertEquals(expected, messages)
    }

    @Test
    fun `a file counts every member of every class once`() {
        val sut = WeightedComplexity(TestConfig("fileLimit" to 0))
        val code = """
            class Outer {
                fun run() {}

                class Nested {
                    fun print() {
                        println("a")
                    }
                }
            }

            fun top() {
                println("top")
            }

            val value = 1
        """.trimIndent()
        val expected = listOf(
            "`Test.kt` has a weighted complexity of 6.0 (limit 0.0, normal bucket). " +
                "Largest parts: `Outer` 3.0, `top()` 2.0, `value` 1.0.",
        )

        val messages = sut.lint(code).map(Finding::message)

        assertEquals(expected, messages)
    }

    @Test
    fun `operators, infix calls, logical operators and elvis are calls`() {
        val sut = WeightedComplexity(TestConfig("classLimit" to 0))
        val code = """
            class Logic {
                fun check(a: Int): Boolean {
                    var x = a + 1
                    x += 2
                    x++
                    val list = listOf(1)
                    val y = list[0]
                    val text: String? = null
                    val name = text ?: "none"
                    val pair = a to name
                    val bits = a shl 1
                    return !(a in 0..y && y !in 0 until x || -a == bits) && x != y
                }
            }
        """.trimIndent()
        val expected = listOf(
            "`Logic` has a weighted complexity of 20.0 (limit 0.0, normal bucket). Largest parts: `check()` 20.0.",
        )

        val messages = sut.lint(code).map(Finding::message)

        assertEquals(expected, messages)
    }

    @Test
    fun `assignments, not-null assertions, casts, type checks and references are not calls`() {
        val sut = WeightedComplexity(TestConfig("classLimit" to 0))
        val code = """
            class Plain {
                fun plain(value: Any?) {
                    var copy = value
                    copy = value!!
                    val text = copy as String
                    val isText = copy is String
                    val reference = String::length
                }
            }
        """.trimIndent()
        val expected = listOf(
            "`Plain` has a weighted complexity of 1.0 (limit 0.0, normal bucket). Largest parts: `plain()` 1.0.",
        )

        val messages = sut.lint(code).map(Finding::message)

        assertEquals(expected, messages)
    }

    @Test
    fun `a comparison with null is a call`() {
        val sut = WeightedComplexity(TestConfig("classLimit" to 0))
        val code = """
            class Check {
                fun isMissing(value: String?): Boolean = value == null
            }
        """.trimIndent()
        val expected = listOf(
            "`Check` has a weighted complexity of 2.0 (limit 0.0, normal bucket). Largest parts: `isMissing()` 2.0.",
        )

        val messages = sut.lint(code).map(Finding::message)

        assertEquals(expected, messages)
    }

    @Test
    fun `composable functions and getters are scored in the composable bucket`() {
        val sut = WeightedComplexity(TestConfig("classLimit" to 2, "composableClassLimit" to 0))
        val code = """
            class Screen {
                fun load() {
                    println("load")
                }

                @Composable
                fun Content() {
                    Text("a")
                }

                val title: String
                    @Composable get() = stringResource(1)
            }
        """.trimIndent()
        val expected = listOf(
            "`Screen` has a weighted complexity of 4.0 (limit 0.0, composable bucket). " +
                "Largest parts: `Content()` 2.0, `title` 2.0.",
        )

        val messages = sut.lint(code).map(Finding::message)

        assertEquals(expected, messages)
    }

    @Test
    fun `a class in the test sources uses the test limit`() {
        val sut = WeightedComplexity(TestConfig("classLimit" to 0, "testClassLimit" to 2))
        val code = """
            class GreeterTest {
                fun greet() {
                    println("a")
                    println("b")
                }
            }
        """.trimIndent()
        val path = createTempDirectory().resolve("src/test/kotlin/GreeterTest.kt").createParentDirectories()
        path.writeText(code)
        val expected = listOf(
            "`GreeterTest` has a weighted complexity of 3.0 (limit 2.0, test bucket). Largest parts: `greet()` 3.0.",
        )

        val messages = sut.lint(compileForTest(path)).map(Finding::message)

        assertEquals(expected, messages)
    }

    @Test
    fun `a score equal to the limit passes`() {
        val sut = WeightedComplexity(TestConfig("classLimit" to 2, "fileLimit" to 2))
        val code = """
            class Greeter {
                fun greet() {
                    println("a")
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `nestWeight sets the weight of each level of nesting`() {
        val sut = WeightedComplexity(TestConfig("classLimit" to 0, "nestWeight" to 0.5))
        val code = """
            class Printer {
                fun print(flag: Boolean) {
                    if (flag) println("a")
                }
            }
        """.trimIndent()
        val expected = listOf(
            "`Printer` has a weighted complexity of 2.5 (limit 0.0, normal bucket). Largest parts: `print()` 2.5.",
        )

        val messages = sut.lint(code).map(Finding::message)

        assertEquals(expected, messages)
    }

    @Test
    fun `chainWeight sets the weight of each earlier call in a chain`() {
        val sut = WeightedComplexity(TestConfig("classLimit" to 0, "chainWeight" to 0.5))
        val code = """
            class Names {
                fun join(items: List<String>): String = items.map { it }.joinToString("")
            }
        """.trimIndent()
        val expected = listOf(
            "`Names` has a weighted complexity of 3.5 (limit 0.0, normal bucket). Largest parts: `join()` 3.5.",
        )

        val messages = sut.lint(code).map(Finding::message)

        assertEquals(expected, messages)
    }

    @Test
    fun `minFunctionScore sets the score of a function without calls`() {
        val sut = WeightedComplexity(TestConfig("classLimit" to 0, "minFunctionScore" to 2))
        val code = """
            class Greeter {
                fun nothing() {}
                fun greet() {
                    println("a")
                }
            }
        """.trimIndent()
        val expected = listOf(
            "`Greeter` has a weighted complexity of 5.0 (limit 0.0, normal bucket). " +
                "Largest parts: `greet()` 3.0, `nothing()` 2.0.",
        )

        val messages = sut.lint(code).map(Finding::message)

        assertEquals(expected, messages)
    }

    @Test
    fun `a property type costs one if arePropertyTypeArgumentsCounted is false`() {
        val sut = WeightedComplexity(TestConfig("classLimit" to 0, "arePropertyTypeArgumentsCounted" to false))
        val code = """
            class Store(val tags: Set<String>) {
                val index: Map<String, List<Int>> = emptyMap()
                val handler: (String) -> Unit = {}
            }
        """.trimIndent()
        val expected = listOf(
            "`Store` has a weighted complexity of 4.0 (limit 0.0, normal bucket). " +
                "Largest parts: `index` 2.0, `tags` 1.0, `handler` 1.0.",
        )

        val messages = sut.lint(code).map(Finding::message)

        assertEquals(expected, messages)
    }

    @Test
    fun `nestedClassScore sets what a nested class adds to its parent`() {
        val sut = WeightedComplexity(TestConfig("classLimit" to 0, "nestedClassScore" to 2.5))
        val code = """
            class Outer {
                fun run() {}

                class Nested {
                    fun print() {}
                }
            }
        """.trimIndent()
        val expected = listOf(
            "`Nested` has a weighted complexity of 1.0 (limit 0.0, normal bucket). Largest parts: `print()` 1.0.",
            "`Outer` has a weighted complexity of 3.5 (limit 0.0, normal bucket). " +
                "Largest parts: `Nested` 2.5, `run()` 1.0.",
        )

        val messages = sut.lint(code).map(Finding::message)

        assertEquals(expected, messages)
    }

    @Test
    fun `a decimal option accepts a quoted number`() {
        val sut = WeightedComplexity(TestConfig("classLimit" to 0, "nestWeight" to "0.5"))
        val code = """
            class Printer {
                fun print(flag: Boolean) {
                    if (flag) println("a")
                }
            }
        """.trimIndent()
        val expected = listOf(
            "`Printer` has a weighted complexity of 2.5 (limit 0.0, normal bucket). Largest parts: `print()` 2.5.",
        )

        val messages = sut.lint(code).map(Finding::message)

        assertEquals(expected, messages)
    }

    @Test
    fun `a decimal option that is not a number fails`() {
        val sut = WeightedComplexity(TestConfig("classLimit" to 0, "nestWeight" to "high"))
        val code = """
            class Greeter {
                fun greet() {}
            }
        """.trimIndent()

        val failure = runCatching { sut.lint(code) }.exceptionOrNull()

        assertIs<IllegalStateException>(failure)
    }
}
