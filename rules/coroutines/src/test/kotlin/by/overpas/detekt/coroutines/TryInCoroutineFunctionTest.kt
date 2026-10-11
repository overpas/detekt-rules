package by.overpas.detekt.coroutines

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals

class TryInCoroutineFunctionTest {

    private val environment = createEnvironment()

    private val sut = TryInCoroutineFunction(Config.empty)

    private val coroutines = """
        package kotlinx.coroutines

        interface CoroutineScope
        interface Job

        fun CoroutineScope.launch(block: suspend CoroutineScope.() -> Unit): Job = TODO()
        fun <T> runBlocking(block: suspend CoroutineScope.() -> T): T = TODO()
        suspend fun delay(timeMillis: Long) {}
    """.trimIndent()

    private val flows = """
        package kotlinx.coroutines.flow

        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Job

        interface Flow<T>

        fun <T> Flow<T>.launchIn(scope: CoroutineScope): Job = TODO()
    """.trimIndent()

    @Test
    fun `a try catch in a suspend function is reported`() {
        val code = """
            suspend fun load(): String =
                try {
                    fetch()
                } catch (e: Exception) {
                    ""
                }

            fun fetch(): String = ""
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a try finally in a suspend function is reported`() {
        val code = """
            suspend fun load() {
                try {
                    open()
                } finally {
                    close()
                }
            }

            fun open() {}
            fun close() {}
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a try in a function that launches a coroutine is reported`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope
            import kotlinx.coroutines.delay
            import kotlinx.coroutines.launch

            class Presenter(private val scope: CoroutineScope) {
                fun start() {
                    scope.launch {
                        try {
                            delay(1)
                        } catch (e: Exception) {
                        }
                    }
                }
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code, coroutines)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a try in a function that calls runBlocking is reported`() {
        val code = """
            import kotlinx.coroutines.delay
            import kotlinx.coroutines.runBlocking

            fun main() {
                try {
                    runBlocking { delay(1) }
                } catch (e: Exception) {
                }
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code, coroutines)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a try in a coroutine scope extension is reported`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope

            fun CoroutineScope.parse(raw: String): Int =
                try {
                    raw.toInt()
                } catch (e: NumberFormatException) {
                    0
                }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code, coroutines)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a try in a function that takes a coroutine scope subtype is reported`() {
        val code = """
            import kotlinx.coroutines.CoroutineScope

            interface ViewScope : CoroutineScope

            fun bind(scope: ViewScope) {
                try {
                    println(scope)
                } finally {
                    println()
                }
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code, coroutines)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a try in a function that calls launchIn is reported`() {
        val code = """
            import kotlinx.coroutines.flow.Flow
            import kotlinx.coroutines.flow.launchIn

            class Presenter(private val events: Flow<Int>) {
                fun start(owner: Owner) {
                    try {
                        events.launchIn(owner.scope)
                    } catch (e: IllegalStateException) {
                    }
                }
            }

            interface Owner {
                val scope: kotlinx.coroutines.CoroutineScope
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code, coroutines, flows)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a try in a function that takes a suspend lambda is reported`() {
        val code = """
            fun retry(block: suspend () -> Unit) {
                try {
                    println(block)
                } catch (e: Exception) {
                }
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a try in a plain function passes`() {
        val code = """
            fun parse(raw: String): Int =
                try {
                    raw.toInt()
                } catch (e: NumberFormatException) {
                    0
                }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a try in a plain local function of a suspend function passes`() {
        val code = """
            suspend fun load(raw: String): Int {
                fun parse(): Int =
                    try {
                        raw.toInt()
                    } catch (e: NumberFormatException) {
                        0
                    }
                return parse()
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a plain function that calls a suspend function passes`() {
        val code = """
            suspend fun fetch(): String = ""

            fun parse(raw: String): Int =
                try {
                    raw.toInt()
                } catch (e: NumberFormatException) {
                    0
                }

            suspend fun load(): Int = parse(fetch())
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a custom scope type is honored`() {
        val sut = TryInCoroutineFunction(TestConfig("scopeTypes" to listOf("Lifecycle")))
        val code = """
            interface Lifecycle

            fun observe(lifecycle: Lifecycle) {
                try {
                    println(lifecycle)
                } catch (e: Exception) {
                }
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }
}
