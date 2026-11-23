package by.overpas.detekt.architecture

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals

class ConstructedCollaboratorTest {

    private val environment = createEnvironment()

    private val sut = ConstructedCollaborator(Config.empty)

    @Test
    fun `a property built from a constructor parameter is reported`() {
        val code = """
            class JobRunner
            class RetryingJobQueue(private val jobRunner: JobRunner)
            class ReportScheduler(jobRunner: JobRunner) {
                private val queue = RetryingJobQueue(jobRunner)
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a property built from a constructor property is reported`() {
        val code = """
            class JobRunner
            class RetryingJobQueue(private val jobRunner: JobRunner)
            class ReportScheduler(private val jobRunner: JobRunner) {
                private val queue = RetryingJobQueue(jobRunner = jobRunner)
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a nested constructor call is reported once`() {
        val code = """
            class JobRunner
            class Retrying(private val queue: RetryingJobQueue)
            class RetryingJobQueue(private val jobRunner: JobRunner)
            class ReportScheduler(jobRunner: JobRunner) {
                private val queue = Retrying(RetryingJobQueue(jobRunner))
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a lazy property built from a constructor parameter is reported`() {
        val code = """
            class JobRunner
            class RetryingJobQueue(private val jobRunner: JobRunner)
            class ReportScheduler(private val jobRunner: JobRunner) {
                private val queue by lazy { RetryingJobQueue(jobRunner) }
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an init block that builds a class from a constructor parameter is reported`() {
        val code = """
            class JobRunner
            class RetryingJobQueue(private val jobRunner: JobRunner) {
                fun start() = Unit
            }
            class ReportScheduler(jobRunner: JobRunner) {
                init {
                    RetryingJobQueue(jobRunner).start()
                }
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a property built without constructor parameters passes`() {
        val code = """
            class RetryingJobQueue(private val name: String)
            class ReportScheduler(private val name: String) {
                private val queue = RetryingJobQueue("work")
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a function call with a constructor parameter passes`() {
        val code = """
            class JobRunner
            class RetryingJobQueue(private val jobRunner: JobRunner)
            fun queueFor(jobRunner: JobRunner) = RetryingJobQueue(jobRunner)
            class ReportScheduler(jobRunner: JobRunner) {
                private val queue = queueFor(jobRunner)
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a class built from a constructor parameter and a lambda parameter passes`() {
        val code = """
            class Cost
            class Part(private val name: String, private val cost: Cost)
            class Parts(names: List<String>, cost: Cost) {
                private val parts = names.map { Part(it, cost) }
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a class built from a constructor parameter and a local value passes`() {
        val code = """
            class Cost
            class Part(private val name: String, private val cost: Cost)
            class Parts(cost: Cost) {
                init {
                    val name = "total"
                    Part(name, cost)
                }
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a data class built from a constructor parameter passes`() {
        val code = """
            data class State(val name: String)
            class Screen(name: String) {
                private val initial = State(name)
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a class from an excluded package built from a constructor parameter passes`() {
        val code = """
            class Parser(pattern: String) {
                private val regex = Regex(pattern)
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an android class built from a constructor parameter passes`() {
        val android = """
            package android.os

            class Looper
            class Handler(looper: Looper)
        """.trimIndent()
        val code = """
            import android.os.Handler
            import android.os.Looper

            class Ticker(looper: Looper) {
                private val handler = Handler(looper)
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code, android)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a class from a package that is not excluded is reported`() {
        val sut = ConstructedCollaborator(TestConfig("excludedPackages" to emptyList<String>()))
        val code = """
            class Parser(pattern: String) {
                private val regex = Regex(pattern)
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a constructor call in a function passes`() {
        val code = """
            class JobRunner
            class RetryingJobQueue(private val jobRunner: JobRunner)
            class ReportScheduler(private val jobRunner: JobRunner) {
                fun queue() = RetryingJobQueue(jobRunner)
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }
}
