package by.overpas.detekt.style

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class SetupWithoutApplyTest {

    private val sut = SetupWithoutApply(Config.empty)

    @Test
    fun `property sets and a call after the creation are reported`() {
        val code = """
            fun create(): SomeObject {
                val obj = SomeObject()
                obj.prop1 = true
                obj.prop2 = "prop2"
                obj.doSmth()
                return obj
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `two calls after the creation are reported`() {
        val code = """
            fun create(): Builder {
                val builder = Builder()
                builder.setName("name")
                builder.setAge(1)
                return builder
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `two uses before an unrelated statement are reported`() {
        val code = """
            fun create(): SomeObject {
                val obj = SomeObject()
                obj.prop1 = true
                obj.prop2 = "prop2"
                log("created")
                obj.doSmth()
                return obj
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a compound assignment is reported as a use`() {
        val code = """
            fun create(): Counter {
                val counter = Counter()
                counter.value += 1
                counter.start()
                return counter
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `one use after the creation passes`() {
        val code = """
            fun create(): SomeObject {
                val obj = SomeObject()
                obj.prop1 = true
                return obj
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `uses split by an unrelated statement pass`() {
        val code = """
            fun create(): SomeObject {
                val obj = SomeObject()
                obj.prop1 = true
                log("created")
                obj.doSmth()
                return obj
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `calls before a call result kept in a local are reported`() {
        val code = """
            fun create(): Request {
                val builder = Builder()
                builder.setName("name")
                builder.setAge(1)
                val request = builder.build()
                return request
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a call result kept in a local is not a use`() {
        val code = """
            fun create(): Request {
                val builder = Builder()
                builder.setName("name")
                val request = builder.build()
                builder.setAge(1)
                return request
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a call result assigned to an existing variable is not a use`() {
        val code = """
            fun create(): Request {
                var request = Request.EMPTY
                val builder = Builder()
                builder.setName("name")
                request = builder.build()
                builder.setAge(1)
                return request
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an object from a factory function passes`() {
        val code = """
            fun create(): SomeObject {
                val obj = createObject()
                obj.prop1 = true
                obj.doSmth()
                return obj
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an object configured in an apply block passes`() {
        val code = """
            fun create(): SomeObject {
                val obj = SomeObject().apply {
                    prop1 = true
                }
                obj.prop2 = "prop2"
                obj.doSmth()
                return obj
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an object passed as an argument and read passes`() {
        val code = """
            fun create(): SomeObject {
                val obj = SomeObject()
                save(obj)
                println(obj.prop1)
                return obj
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a class property passes`() {
        val code = """
            class Holder {
                val obj = SomeObject()

                fun configure() {
                    obj.prop1 = true
                    obj.doSmth()
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `uses under the configured minimum pass`() {
        val sut = SetupWithoutApply(TestConfig("minUseCount" to 3))
        val code = """
            fun create(): SomeObject {
                val obj = SomeObject()
                obj.prop1 = true
                obj.doSmth()
                return obj
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }
}
