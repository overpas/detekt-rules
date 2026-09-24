package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class FileStructureTest {

    private val sut = FileStructure(Config.empty)

    @Test
    fun `a file in the full top-level order passes`() {
        val code = """
            const val LIMIT = 1

            val default = Box(LIMIT)

            typealias Boxes = List<Box>

            interface Container {

                val size: Int
            }

            class Box(override val size: Int) : Container

            fun Box.grow(): Box = Box(size + 1)

            fun emptyBox(): Box = Box(0)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a constant after a property is reported`() {
        val code = """
            val default = LIMIT

            const val LIMIT = 1
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a property after an interface is reported`() {
        val code = """
            interface Container

            val default = 1
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an interface after a class is reported`() {
        val code = """
            class Box : Container

            interface Container
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a class after a function is reported`() {
        val code = """
            fun emptyBox(): Box = Box()

            class Box
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a top-level extension property after a class passes`() {
        val code = """
            class Box(val size: Int)

            val Box.isEmpty: Boolean get() = size == 0
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a top-level extension property before a class is reported`() {
        val code = """
            val Box.isEmpty: Boolean get() = size == 0

            class Box(val size: Int)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a second top-level interface is reported`() {
        val code = """
            interface Container

            interface Holder
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a second top-level object is reported`() {
        val code = """
            class Box

            object Boxes
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `several top-level annotation classes pass`() {
        val code = """
            annotation class Main

            annotation class Io

            class Dispatchers
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a class in the full member order passes`() {
        val code = """
            class Box(size: Int) {

                private val initial = size

                val current = initial

                init {
                    check(size > 0)
                }

                constructor() : this(1)

                fun grow(): Box = Box(current + 1)

                companion object {

                    const val LIMIT = 1
                }

                interface Listener

                class Content
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a private property after a non-private property is reported`() {
        val code = """
            class Box {

                val current = 1

                private val initial = 0
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a property after an init block is reported`() {
        val code = """
            class Box {

                init {
                    println()
                }

                val current = 1
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a secondary constructor after a function is reported`() {
        val code = """
            class Box(val size: Int) {

                fun grow(): Box = Box(size + 1)

                constructor() : this(1)
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a function after a companion object is reported`() {
        val code = """
            class Box {

                companion object

                fun grow() = Unit
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a companion object after a nested interface is reported`() {
        val code = """
            class Box {

                interface Listener

                companion object
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a nested interface after a nested class is reported`() {
        val code = """
            class Box {

                class Content

                interface Listener
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an enum class with entries first passes`() {
        val code = """
            enum class Kind(val label: String) {
                Totp("totp"),
                Hotp("hotp"),
                ;

                fun describe(): String = label
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an object body follows the class order`() {
        val code = """
            object Boxes {

                fun create() = Unit

                val default = 1
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a companion object body follows the class order`() {
        val code = """
            class Box {

                companion object {

                    val LIMIT = 1

                    private val initial = 0
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an interface in the full member order passes`() {
        val code = """
            interface Container {

                val size: Int

                fun isEmpty(): Boolean = size == 0

                companion object

                interface Factory

                class Default(override val size: Int) : Container
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an interface property after a function is reported`() {
        val code = """
            interface Container {

                fun isEmpty(): Boolean

                val size: Int
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an interface nested interface after a nested class is reported`() {
        val code = """
            interface Container {

                class Default

                interface Factory
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a private interface property does not change the interface order`() {
        val code = """
            interface Container {

                val size: Int

                private val half: Int get() = size / 2
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }
}
