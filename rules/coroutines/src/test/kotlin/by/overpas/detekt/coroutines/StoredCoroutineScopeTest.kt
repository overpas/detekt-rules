package by.overpas.detekt.coroutines

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class StoredCoroutineScopeTest {

    private val sut = StoredCoroutineScope(Config.empty)

    @Test
    fun `an injected scope is reported`() {
        val code = """
            class Repository(private val scope: CoroutineScope)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a constructor parameter that is not a property is reported`() {
        val code = """
            class Repository(scope: CoroutineScope, api: Api)
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a created scope property is reported`() {
        val code = """
            class Repository {
                private val scope = CoroutineScope(Dispatchers.Default)
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a lazily created scope is reported`() {
        val code = """
            class Repository {
                private val scope by lazy { MainScope() }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a top-level scope is reported`() {
        val code = """
            val appScope: CoroutineScope = MainScope()
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a class that is a scope is reported`() {
        val code = """
            class Manager(context: CoroutineContext) : CoroutineScope {
                override val coroutineContext = context
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a function-local scope passes`() {
        val code = """
            fun run() {
                val scope = CoroutineScope(Dispatchers.Default)
                scope.launch { work() }
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a scope accessor without storage passes`() {
        val code = """
            val ComponentContext.instanceScope: CoroutineScope
                get() = instanceKeeper.getOrCreate { InstanceScope() }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a scope parameter of a function passes`() {
        val code = """
            fun CoroutineScope.start(scope: CoroutineScope) = Unit
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an allowed class passes`() {
        val sut = StoredCoroutineScope(TestConfig("allowedClasses" to listOf("Executor")))
        val code = """
            class Executor(
                private val executionScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
            ) : CoroutineScope by executionScope {
                private val childScope = CoroutineScope(Dispatchers.Main)
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }
}
