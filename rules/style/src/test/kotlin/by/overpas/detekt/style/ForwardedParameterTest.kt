package by.overpas.detekt.style

import dev.detekt.api.Config
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class ForwardedParameterTest {

    private val sut = ForwardedParameter(Config.empty)

    @Test
    fun `an object parameter passed positionally is reported`() {
        val code = """
            fun save(user: User) {
                repository.store(user)
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an object parameter passed as a named argument is reported`() {
        val code = """
            fun save(user: User) {
                store(user = user)
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a function type parameter passed on is reported`() {
        val code = """
            fun listen(onEvent: (Event) -> Unit) {
                bus.subscribe(onEvent)
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a parameter passed inside a lambda is reported`() {
        val code = """
            fun save(user: User) {
                scope.launch { store(user) }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a parameter passed from an expression body is reported`() {
        val code = """
            fun save(user: User) = store(user)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a parameter used as a receiver is not reported`() {
        val code = """
            fun save(user: User) {
                user.store()
                user?.validate()
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a primitive parameter passed on is not reported`() {
        val code = """
            fun load(id: Int, name: String?) {
                fetch(id, name)
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an object created inside the function is not reported`() {
        val code = """
            fun save() {
                val user = createUser()
                store(user)
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a constructor call is not reported`() {
        val code = """
            fun wrap(items: List<Item>) = State(items = items)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a composable function is not reported`() {
        val code = """
            @Composable
            fun Screen(modifier: Modifier) {
                Content(modifier = modifier)
                content(modifier)
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an override function is not reported`() {
        val code = """
            class Factory : Creator {
                override fun create(context: Context) = delegate.create(context)
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a lambda parameter that shadows the parameter is not reported`() {
        val code = """
            fun save(user: User) {
                users.forEach { user -> store(user) }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a nested function parameter is reported once`() {
        val code = """
            fun outer(user: User) {
                fun inner(user: User) {
                    store(user)
                }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a parameter passed to a scope function is not reported`() {
        val code = """
            fun describe(user: User): String =
                with(user) { name }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a parameter passed to a precondition is not reported`() {
        val code = """
            fun save(user: User?) {
                requireNotNull(user)
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a parameter passed to a contract is not reported`() {
        val code = """
            fun perform(block: () -> Unit) {
                contract { callsInPlace(block) }
                block()
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a parameter passed to a collection function is not reported`() {
        val code = """
            fun pair(items: List<Item>, other: List<Item>) =
                items.zip(other)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a parameter passed to a collection factory is not reported`() {
        val code = """
            fun wrap(user: User) =
                listOf(user)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }
}
