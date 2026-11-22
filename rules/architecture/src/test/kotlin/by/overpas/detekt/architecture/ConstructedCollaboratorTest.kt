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
            class WorkManager
            class AndroidWorkQueue(private val workManager: WorkManager)
            class Scheduler(workManager: WorkManager) {
                private val queue = AndroidWorkQueue(workManager)
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a property built from a constructor property is reported`() {
        val code = """
            class WorkManager
            class AndroidWorkQueue(private val workManager: WorkManager)
            class Scheduler(private val workManager: WorkManager) {
                private val queue = AndroidWorkQueue(workManager = workManager)
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a nested constructor call is reported once`() {
        val code = """
            class WorkManager
            class Retrying(private val queue: AndroidWorkQueue)
            class AndroidWorkQueue(private val workManager: WorkManager)
            class Scheduler(workManager: WorkManager) {
                private val queue = Retrying(AndroidWorkQueue(workManager))
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a lazy property built from a constructor parameter is reported`() {
        val code = """
            class WorkManager
            class AndroidWorkQueue(private val workManager: WorkManager)
            class Scheduler(private val workManager: WorkManager) {
                private val queue by lazy { AndroidWorkQueue(workManager) }
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an init block that builds a class from a constructor parameter is reported`() {
        val code = """
            class WorkManager
            class AndroidWorkQueue(private val workManager: WorkManager) {
                fun start() = Unit
            }
            class Scheduler(workManager: WorkManager) {
                init {
                    AndroidWorkQueue(workManager).start()
                }
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a property built without constructor parameters passes`() {
        val code = """
            class AndroidWorkQueue(private val name: String)
            class Scheduler(private val name: String) {
                private val queue = AndroidWorkQueue("work")
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a function call with a constructor parameter passes`() {
        val code = """
            class WorkManager
            class AndroidWorkQueue(private val workManager: WorkManager)
            fun queueOf(workManager: WorkManager) = AndroidWorkQueue(workManager)
            class Scheduler(workManager: WorkManager) {
                private val queue = queueOf(workManager)
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
            class WorkManager
            class AndroidWorkQueue(private val workManager: WorkManager)
            class Scheduler(private val workManager: WorkManager) {
                fun queue() = AndroidWorkQueue(workManager)
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }
}
