package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class LaunchInInitializerTest {

    private val sut = LaunchInInitializer(Config.empty)

    @Test
    fun `a launch in an init block is reported`() {
        val code = """
            class Component(scope: CoroutineScope) {
                init {
                    scope.launch { load() }
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a launchIn in an init block is reported`() {
        val code = """
            class Component(scope: CoroutineScope, labels: Flow<Label>) {
                init {
                    labels
                        .onEach(::handle)
                        .launchIn(scope)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an async in a property initializer is reported`() {
        val code = """
            class Loader(scope: CoroutineScope) {
                private val data = scope.async { load() }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a launch in a registered callback passes`() {
        val code = """
            class Component(scope: CoroutineScope, lifecycle: Lifecycle) {
                init {
                    lifecycle.doOnCreate {
                        scope.launch { load() }
                    }
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a launch in a lazy property passes`() {
        val code = """
            class Loader(scope: CoroutineScope) {
                private val data by lazy { scope.async { load() } }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a launch in a function passes`() {
        val code = """
            class Component(private val scope: CoroutineScope) {
                fun onClick() {
                    val job = scope.launch { load() }
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a custom launch call is honored`() {
        val sut = LaunchInInitializer(TestConfig("launchCalls" to listOf("start")))
        val code = """
            class Component(scope: CoroutineScope) {
                init {
                    scope.start { load() }
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }
}
