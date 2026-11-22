package by.overpas.detekt.architecture

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class StoreTypeLeakTest {

    private val sut = StoreTypeLeak(Config.empty)

    @Test
    fun `a message import outside the store package is reported`() {
        val code = """
            package app.feature.sync.toggle.overview

            import app.feature.sync.toggle.SyncToggleStore.Message

            class SyncOverviewLoader(private val dispatch: (Message) -> Unit)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a qualified action outside the store package is reported`() {
        val code = """
            package app.feature.sync

            import app.feature.sync.toggle.SyncToggleStore

            class Loader(private val first: SyncToggleStore.Action, private val second: SyncToggleStore.Action)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a qualified message expression outside the store package is reported`() {
        val code = """
            package app.feature.sync.loader

            import app.feature.sync.toggle.SyncToggleStore

            val loaded = SyncToggleStore.Message.Loaded
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a store type in the store package passes`() {
        val code = """
            package app.feature.sync.toggle

            class SyncToggleExecutorFactory(private val dispatch: (SyncToggleStore.Message) -> Unit)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a store type in the handlers package passes`() {
        val code = """
            package app.feature.sync.toggle.handlers

            import app.feature.sync.toggle.SyncToggleStore.Action
            import app.feature.sync.toggle.SyncToggleStore.Message

            class OnResumeHandler(private val dispatch: (Message) -> Unit, private val forward: (Action) -> Unit)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an intent, a state and a label in the component package pass`() {
        val code = """
            package app.feature.sync

            import app.feature.sync.toggle.SyncToggleStore

            class SyncComponent(private val store: SyncToggleStore) {
                fun onBackClick() = store.accept(SyncToggleStore.Intent.OnBackClick)
                fun handle(label: SyncToggleStore.Label, state: SyncToggleStore.State) = Unit
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a message in the component package is reported`() {
        val code = """
            package app.feature.sync

            import app.feature.sync.toggle.SyncToggleStore

            class SyncComponent(private val message: SyncToggleStore.Message)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a state in the ui package passes`() {
        val code = """
            package app.feature.sync.ui

            import app.feature.sync.toggle.SyncToggleStore.State

            fun SyncUi(state: State) = Unit
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an intent in the ui package is reported`() {
        val code = """
            package app.feature.sync.ui

            import app.feature.sync.toggle.SyncToggleStore.Intent

            fun SyncUi(onIntent: (Intent) -> Unit) = Unit
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a nested type of a class without the store suffix passes`() {
        val code = """
            package app.feature.sync.ui

            import app.feature.sync.toggle.SyncToggleExecutor.Message

            fun SyncUi(message: Message) = Unit
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `custom component types are honored`() {
        val sut = StoreTypeLeak(TestConfig("componentTypes" to listOf("State", "Label")))
        val code = """
            package app.feature.sync

            import app.feature.sync.toggle.SyncToggleStore

            class SyncComponent(private val intent: SyncToggleStore.Intent)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }
}
