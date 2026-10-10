package by.overpas.detekt.compose

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals

class PresentationEntityPassedDownTest {

    private val environment = createEnvironment()

    private val sut = PresentationEntityPassedDown(Config.empty)

    @Test
    fun `a component passed to a composable in the same file is reported`() {
        val code = """
            annotation class Composable
            interface AccountsState
            interface AccountsComponent {
                val state: AccountsState
            }

            @Composable
            fun AccountsUi(component: AccountsComponent) {
                AccountsContent(state = component.state, component = component)
            }

            @Composable
            private fun AccountsContent(state: AccountsState, component: AccountsComponent) {
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `each component argument passed to a composable in the same file is reported`() {
        val code = """
            annotation class Composable
            interface AccountsListComponent {
                fun onAccountClick(id: String)
            }
            interface AccountsMenuComponent {
                fun onAccountLongClick(id: String)
            }
            interface AccountsComponent : AccountsListComponent, AccountsMenuComponent

            @Composable
            private fun AccountsContent(component: AccountsComponent) {
                AccountRow(id = "1", listComponent = component, menuComponent = component)
            }

            @Composable
            private fun AccountRow(id: String, listComponent: AccountsListComponent, menuComponent: AccountsMenuComponent) {
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(2, findings.size)
    }

    @Test
    fun `a component passed from a content lambda is reported`() {
        val code = """
            annotation class Composable
            interface AccountsComponent

            @Composable
            fun Column(content: @Composable () -> Unit) {
            }

            @Composable
            fun AccountsUi(component: AccountsComponent) {
                Column {
                    AccountRow(component = component)
                }
            }

            @Composable
            private fun AccountRow(component: AccountsComponent) {
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `method references and lambdas that call the component pass`() {
        val code = """
            annotation class Composable
            interface AccountsComponent {
                fun onAccountClick(id: String)
                fun onClear()
            }

            @Composable
            fun AccountsUi(component: AccountsComponent) {
                AccountsContent(onAccountClick = component::onAccountClick, onClear = { component.onClear() })
            }

            @Composable
            private fun AccountsContent(onAccountClick: (String) -> Unit, onClear: () -> Unit) {
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a child component passed to the entrypoint of another screen passes`() {
        val accounts = """
            annotation class Composable
            interface AccountsComponent

            @Composable
            fun AccountsUi(component: AccountsComponent) {
            }
        """.trimIndent()
        val code = """
            interface RootComponent {
                val child: Child
            }
            sealed interface Child {
                class Accounts(val component: AccountsComponent) : Child
            }

            @Composable
            fun RootUi(component: RootComponent) {
                when (val child = component.child) {
                    is Child.Accounts -> AccountsUi(component = child.component)
                }
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code, accounts)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a component passed as a key to a library composable passes`() {
        val library = """
            annotation class Composable

            @Composable
            fun LaunchedEffect(key: Any?, block: () -> Unit) {
            }
        """.trimIndent()
        val code = """
            interface AccountsComponent {
                fun onStart()
            }

            @Composable
            fun AccountsUi(component: AccountsComponent) {
                LaunchedEffect(component) { component.onStart() }
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code, library)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a subtype of an entity type is reported`() {
        val decompose = """
            package com.arkivanov.decompose

            interface ComponentContext
        """.trimIndent()
        val code = """
            import com.arkivanov.decompose.ComponentContext

            annotation class Composable
            class Accounts(context: ComponentContext) : ComponentContext by context

            @Composable
            fun AccountsUi(accounts: Accounts) {
                AccountsContent(accounts)
            }

            @Composable
            private fun AccountsContent(accounts: Accounts) {
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code, decompose)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a type with an entity suffix on its supertype is reported`() {
        val code = """
            annotation class Composable
            abstract class ViewModel
            class Accounts : ViewModel()

            @Composable
            fun AccountsUi(accounts: Accounts) {
                AccountsContent(accounts)
            }

            @Composable
            private fun AccountsContent(accounts: Accounts) {
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a nullable component passed down is reported`() {
        val code = """
            annotation class Composable
            interface AccountsComponent

            @Composable
            fun AccountsUi(component: AccountsComponent?) {
                AccountsContent(component)
            }

            @Composable
            private fun AccountsContent(component: AccountsComponent?) {
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an excluded type passes`() {
        val saveable = """
            package androidx.compose.runtime.saveable

            interface SaveableStateHolder
        """.trimIndent()
        val code = """
            import androidx.compose.runtime.saveable.SaveableStateHolder

            annotation class Composable

            @Composable
            fun AccountsUi(holder: SaveableStateHolder) {
                AccountsContent(holder)
            }

            @Composable
            private fun AccountsContent(holder: SaveableStateHolder) {
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code, saveable)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a component passed down from a function that is not composable passes`() {
        val code = """
            interface AccountsComponent

            fun bind(component: AccountsComponent) {
                register(component)
            }

            private fun register(component: AccountsComponent) {
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a configured suffix is reported`() {
        val sut = PresentationEntityPassedDown(TestConfig("entitySuffixes" to listOf("Coordinator")))
        val code = """
            annotation class Composable
            interface AccountsCoordinator

            @Composable
            fun AccountsUi(coordinator: AccountsCoordinator) {
                AccountsContent(coordinator)
            }

            @Composable
            private fun AccountsContent(coordinator: AccountsCoordinator) {
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }
}
