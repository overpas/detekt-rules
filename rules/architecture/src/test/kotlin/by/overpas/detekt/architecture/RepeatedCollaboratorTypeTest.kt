package by.overpas.detekt.architecture

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals

class RepeatedCollaboratorTypeTest {

    private val environment = createEnvironment()

    private val sut = RepeatedCollaboratorType(Config.empty)

    @Test
    fun `two parameters of the same class are reported`() {
        val code = """
            interface Repository
            class Interactor(private val users: Repository, private val orders: Repository)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a group of three parameters of the same class is reported once`() {
        val code = """
            interface Repository
            class Interactor(users: Repository, orders: Repository, items: Repository)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `parameters of different classes pass`() {
        val code = """
            interface Users
            interface Orders
            class Interactor(users: Users, orders: Orders)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a nullable and a non-null parameter of the same class are reported`() {
        val code = """
            interface Repository
            class Interactor(users: Repository, orders: Repository?)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `type arguments with a common supertype are reported`() {
        val code = """
            interface Entity
            class User : Entity
            class Order : Entity
            interface Store<T>
            class Interactor(users: Store<User>, orders: Store<Order>)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `type arguments in a subtype relation are reported`() {
        val code = """
            open class User
            class Admin : User()
            interface Store<T>
            class Interactor(users: Store<User>, admins: Store<Admin>)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `type arguments without a common supertype pass`() {
        val code = """
            class User
            class Order
            interface Store<T>
            class Interactor(users: Store<User>, orders: Store<Order>)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `type arguments with only an ignored common supertype pass`() {
        val code = """
            interface Store<T>
            class Interactor(names: Store<String>, counts: Store<Int>)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `nested type arguments without a common supertype pass`() {
        val code = """
            class User
            class Order
            interface Store<T>
            class Interactor(users: Store<List<User>>, orders: Store<List<Order>>)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `parameters of excluded types pass`() {
        val code = """
            class Settings(
                first: Int,
                second: Int,
                third: String,
                fourth: String,
                fifth: List<String>,
                sixth: Set<Int>,
                seventh: Map<String, Int>,
                eighth: Map<Int, String>,
            )
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `function type parameters pass`() {
        val code = """
            class Button(onClick: () -> Unit, onLongClick: () -> Unit)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a data class passes`() {
        val code = """
            class Account
            data class Transfer(val from: Account, val to: Account)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a data class is reported when data classes are not ignored`() {
        val sut = RepeatedCollaboratorType(TestConfig("areDataClassesIgnored" to false))
        val code = """
            class Account
            data class Transfer(val from: Account, val to: Account)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a secondary constructor is reported`() {
        val code = """
            interface Repository
            class Interactor {
                constructor(users: Repository, orders: Repository)
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `custom excluded types are honored`() {
        val sut = RepeatedCollaboratorType(TestConfig("excludedTypes" to listOf("Repository")))
        val code = """
            interface Repository
            class Interactor(users: Repository, orders: Repository)
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }
}
