package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class LinearContainsCheckTest {

    private val sut = LinearContainsCheck(Config.empty)

    @Test
    fun `a check on a list literal is reported`() {
        val code = """
            fun known(id: Int): Boolean = id in listOf(1, 2, 3)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a check on a list parameter is reported`() {
        val code = """
            fun unknown(name: String, names: List<String>): Boolean = name !in names
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a contains call on an array list property is reported`() {
        val code = """
            class Cache {
                private val ids = ArrayList<Int>()

                fun holds(id: Int): Boolean = ids.contains(id)
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a check on an array parameter is reported`() {
        val code = """
            fun flagged(args: Array<String>): Boolean = "--write" in args
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a check on a local list is reported`() {
        val code = """
            fun known(id: Int): Boolean {
                val ids = mutableListOf(1, 2, 3)
                return id in ids
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a check on a toList call is reported`() {
        val code = """
            fun known(id: Int, ids: Sequence<Int>): Boolean = id in ids.toList()
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a safe contains call is reported`() {
        val code = """
            fun known(id: Int, ids: List<Int>?): Boolean = ids?.contains(id) == true
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a check on a nullable list property is reported`() {
        val code = """
            class Cache {
                private val ids: List<Int>? = null

                fun holds(id: Int): Boolean = ids?.contains(id) == true
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a check on a constructor parameter is reported`() {
        val code = """
            class Cache(private val ids: List<Int>) {

                fun holds(id: Int): Boolean = id in ids
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a check on a top level list is reported`() {
        val code = """
            private val ids = listOf(1, 2, 3)

            fun known(id: Int): Boolean = id in ids
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a when condition on a list is reported`() {
        val code = """
            fun label(id: Int, ids: List<Int>): String = when (id) {
                in ids -> "known"
                else -> "new"
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `every check is reported`() {
        val code = """
            fun known(id: Int, ids: List<Int>): Boolean = id in ids && ids.contains(id)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(2, findings.size)
    }

    @Test
    fun `a check on a set passes`() {
        val code = """
            fun known(id: Int, ids: Set<Int>): Boolean = id in ids
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a check on a set literal passes`() {
        val code = """
            fun known(id: Int): Boolean = id in setOf(1, 2, 3)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a check on a map passes`() {
        val code = """
            fun known(id: Int, names: Map<Int, String>): Boolean = id in names
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a check on a toSet call passes`() {
        val code = """
            fun known(id: Int, ids: List<Int>): Boolean {
                val known = ids.toSet()
                return id in known
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a check on a range passes`() {
        val code = """
            fun digit(char: Char): Boolean = char in '0'..'9'
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a check on a named range passes`() {
        val code = """
            fun valid(counter: Int): Boolean = counter !in MIN..MAX
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a contains call on a string passes`() {
        val code = """
            fun secret(text: String): Boolean = text.contains("secret")
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a check on an unknown call passes`() {
        val code = """
            fun known(id: Int): Boolean = id in idsOf()
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a containsAll call passes`() {
        val code = """
            fun known(ids: List<Int>, others: List<Int>): Boolean = ids.containsAll(others)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a local set shadowing a list parameter passes`() {
        val code = """
            fun known(id: Int, ids: List<Int>): Boolean {
                val ids = ids.toSet()
                return id in ids
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an iteration over an array passes`() {
        val code = """
            fun total(values: Array<Int>): Int {
                var sum = 0
                for (value in values) sum += value
                return sum
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an iteration over a list literal passes`() {
        val code = """
            fun print() {
                for (id in listOf(1, 2, 3)) println(id)
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an iteration over a list property passes`() {
        val code = """
            class Cache(private val ids: List<Int>) {

                fun print() {
                    for (id in ids) println(id)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a destructuring iteration passes`() {
        val code = """
            fun print(pairs: List<Pair<Int, String>>) {
                for ((id, name) in pairs) println("${'$'}id ${'$'}name")
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an iteration over indices passes`() {
        val code = """
            fun print(ids: List<Int>) {
                for (index in 0 until ids.size) println(ids[index])
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an iteration around a check is reported once`() {
        val code = """
            fun print(ids: List<Int>, known: List<Int>) {
                for (id in ids) {
                    if (id in known) println(id)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `the linear types are configurable`() {
        val sut = LinearContainsCheck(TestConfig("linearTypes" to listOf("Deque")))
        val code = """
            fun known(id: Int, ids: Deque<Int>): Boolean = id in ids
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `the linear factories are configurable`() {
        val sut = LinearContainsCheck(TestConfig("linearFactories" to listOf("rowsOf")))
        val code = """
            fun known(id: Int): Boolean = id in rowsOf(1, 2, 3)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }
}
