package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class RunBlockingOutsideMainTest {

    private val sut = RunBlockingOutsideMain(Config.empty)

    @Test
    fun `runBlocking in main passes`() {
        val code = """
            fun main() {
                runBlocking {
                    run()
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `runBlocking in a lambda in main passes`() {
        val code = """
            fun main(args: Array<String>) {
                exitProcess(runBlocking { run(args) })
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `runBlocking in another function is reported`() {
        val code = """
            fun load(): Data =
                runBlocking {
                    repository.load()
                }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `runBlocking in a member main is reported`() {
        val code = """
            class App {
                fun main() {
                    runBlocking { run() }
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `runBlocking in a property initializer is reported`() {
        val code = """
            val data = runBlocking { repository.load() }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a custom allowed function is honored`() {
        val sut = RunBlockingOutsideMain(TestConfig("allowedFunctions" to listOf("bridge")))
        val code = """
            fun bridge() {
                runBlocking { run() }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }
}
