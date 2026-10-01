package by.overpas.detekt.coroutines

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class SharingInFunctionTest {

    private val sut = SharingInFunction(Config.empty)

    @Test
    fun `stateIn in a property initializer passes`() {
        val code = """
            class Holder(scope: CoroutineScope, flow: Flow<Int>) {
                val state: StateFlow<Int> = flow.stateIn(scope, SharingStarted.Eagerly, 0)
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `stateIn in an init block passes`() {
        val code = """
            class Holder(scope: CoroutineScope, flow: Flow<Int>) {
                val state: StateFlow<Int>

                init {
                    state = flow.stateIn(scope, SharingStarted.Eagerly, 0)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `stateIn in a function is reported`() {
        val code = """
            class Holder(private val scope: CoroutineScope, private val flow: Flow<Int>) {
                fun state(): StateFlow<Int> =
                    flow.stateIn(scope, SharingStarted.Eagerly, 0)
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `shareIn in a getter is reported`() {
        val code = """
            class Holder(private val scope: CoroutineScope, private val flow: Flow<Int>) {
                val events: SharedFlow<Int>
                    get() = flow.shareIn(scope, SharingStarted.Eagerly)
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `stateIn in an extension wrapper passes`() {
        val code = """
            fun <T> Flow<T>.stateInComponent(initial: T): StateFlow<T> =
                stateIn(instanceScope, SharingStarted.Eagerly, initial)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a custom sharing call is honored`() {
        val sut = SharingInFunction(TestConfig("sharingCalls" to listOf("share")))
        val code = """
            fun events(flow: Flow<Int>) =
                flow.share()
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }
}
