package by.overpas.detekt.architecture

import dev.detekt.api.Config
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals

class CatchWithoutResultTypeTest {

    private val environment = createEnvironment()

    private val sut = CatchWithoutResultType(Config.empty)

    @Test
    fun `a nullable string with a try catch finally is reported`() {
        val code = """
            class ReaderException : Exception()
            class Reader {
                fun read(bytes: ByteArray): String = TODO()
                fun reset() {}
            }

            fun decode(reader: Reader, bytes: ByteArray): String? {
                return try {
                    reader.read(bytes)
                } catch (_: ReaderException) {
                    null
                } finally {
                    reader.reset()
                }
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an extension that returns a nullable class is reported`() {
        val code = """
            class EncodingException : Exception()
            class Secret(val bytes: ByteArray)

            fun String.decoded(): ByteArray = TODO()

            internal fun String?.toSecret(): Secret? {
                if (this == null) return null
                return try {
                    Secret(decoded())
                } catch (_: EncodingException) {
                    null
                }
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a generic return type with a catch that throws again is reported`() {
        val code = """
            class ExecutionException(override val cause: Throwable?) : Exception()
            class Task<T>(private val block: () -> T) {
                fun get(): T = block()
            }

            fun <T> runOnUiThread(block: () -> T): T {
                val task = Task(block)
                return try {
                    task.get()
                } catch (e: ExecutionException) {
                    throw e.cause ?: e
                }
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `kotlin Result passes`() {
        val code = """
            class ExecutionException(override val cause: Throwable?) : Exception()
            class Task<T>(private val block: () -> T) {
                fun get(): T = block()
            }

            fun <T> runOnUiThread(block: () -> T): Result<T> {
                val task = Task(block)
                return try {
                    Result.success(task.get())
                } catch (e: ExecutionException) {
                    Result.failure(e.cause ?: e)
                }
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a sealed interface with a try inside a lambda passes`() {
        val code = """
            sealed interface GoogleMigrationResult {
                data class Success(val payload: ByteArray) : GoogleMigrationResult
                data object Invalid : GoogleMigrationResult
            }

            fun dataValue(text: String): String? = text.takeIf { it.isNotEmpty() }
            fun decodeBase64(text: String): ByteArray = TODO()

            fun parse(text: String): GoogleMigrationResult {
                val payload = dataValue(text)?.let { data ->
                    try {
                        decodeBase64(data)
                    } catch (_: IllegalArgumentException) {
                        null
                    }
                }
                return payload?.let(GoogleMigrationResult::Success) ?: GoogleMigrationResult.Invalid
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a try inside a lambda counts for the enclosing function`() {
        val code = """
            fun parse(text: String?): Int {
                val value = text?.let { data ->
                    try {
                        data.toInt()
                    } catch (_: NumberFormatException) {
                        null
                    }
                }
                return value ?: 0
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a sealed class passes`() {
        val code = """
            sealed class Parsed {
                data class Value(val number: Int) : Parsed()
                data object Invalid : Parsed()
            }

            fun parse(text: String): Parsed =
                try {
                    Parsed.Value(text.toInt())
                } catch (_: NumberFormatException) {
                    Parsed.Invalid
                }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an enum passes`() {
        val code = """
            enum class Status { VALID, INVALID }

            fun validate(text: String): Status =
                try {
                    text.toInt()
                    Status.VALID
                } catch (_: NumberFormatException) {
                    Status.INVALID
                }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an inferred result type passes`() {
        val code = """
            fun parse(text: String) =
                try {
                    Result.success(text.toInt())
                } catch (e: NumberFormatException) {
                    Result.failure(e)
                }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a type alias of a result type passes`() {
        val code = """
            typealias Parsed = Result<Int>

            fun parse(text: String): Parsed =
                try {
                    Result.success(text.toInt())
                } catch (e: NumberFormatException) {
                    Result.failure(e)
                }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a type alias of a nullable result type is reported`() {
        val code = """
            typealias Parsed = Result<Int>?

            fun parse(text: String): Parsed =
                try {
                    Result.success(text.toInt())
                } catch (_: NumberFormatException) {
                    null
                }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a nullable kotlin Result is reported`() {
        val code = """
            fun parse(text: String): Result<Int>? =
                try {
                    Result.success(text.toInt())
                } catch (_: NumberFormatException) {
                    null
                }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a nullable sealed interface is reported`() {
        val code = """
            sealed interface Parsed {
                data class Value(val number: Int) : Parsed
            }

            fun parse(text: String): Parsed? =
                try {
                    Parsed.Value(text.toInt())
                } catch (_: NumberFormatException) {
                    null
                }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a subclass of a sealed interface is reported`() {
        val code = """
            sealed interface Parsed {
                data class Value(val number: Int) : Parsed
            }

            fun parse(text: String): Parsed.Value =
                try {
                    Parsed.Value(text.toInt())
                } catch (_: NumberFormatException) {
                    Parsed.Value(0)
                }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an open class is reported`() {
        val code = """
            open class Parsed(val number: Int)

            fun parse(text: String): Parsed =
                try {
                    Parsed(text.toInt())
                } catch (_: NumberFormatException) {
                    Parsed(0)
                }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a function without a return type is reported`() {
        val code = """
            fun save(text: String) {
                try {
                    text.toInt()
                } catch (e: NumberFormatException) {
                    println(e)
                }
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a collection is reported`() {
        val code = """
            fun parse(text: String): List<Int> =
                try {
                    listOf(text.toInt())
                } catch (_: NumberFormatException) {
                    emptyList()
                }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `an override is reported`() {
        val code = """
            class Save(private val text: String) : Runnable {
                override fun run() {
                    try {
                        text.toInt()
                    } catch (e: NumberFormatException) {
                        println(e)
                    }
                }
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a function with two try catch expressions is reported once`() {
        val code = """
            fun parse(first: String, second: String): Int {
                val left = try {
                    first.toInt()
                } catch (_: NumberFormatException) {
                    0
                }
                val right = try {
                    second.toInt()
                } catch (_: NumberFormatException) {
                    0
                }
                return left + right
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `the finding is at the function name`() {
        val code = """
            fun parse(text: String): Int =
                try {
                    text.toInt()
                } catch (_: NumberFormatException) {
                    0
                }
        """.trimIndent()

        val line = sut.lintWithContext(environment, code)
            .single()
            .entity
            .location
            .source
            .line

        assertEquals(1, line)
    }

    @Test
    fun `a local function is reported on its own`() {
        val code = """
            fun outer(text: String): Result<Int> {
                fun inner(): Int =
                    try {
                        text.toInt()
                    } catch (_: NumberFormatException) {
                        0
                    }
                return Result.success(inner())
            }
        """.trimIndent()

        val line = sut.lintWithContext(environment, code)
            .single()
            .entity
            .location
            .source
            .line

        assertEquals(2, line)
    }

    @Test
    fun `a local function with a result type does not affect the enclosing function`() {
        val code = """
            fun outer(text: String): Int {
                fun inner(): Result<Int> =
                    try {
                        Result.success(text.toInt())
                    } catch (e: NumberFormatException) {
                        Result.failure(e)
                    }
                return inner().getOrDefault(0)
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a try inside an anonymous function counts for the enclosing function`() {
        val code = """
            fun outer(text: String): Int {
                val parse = fun(): Int =
                    try {
                        text.toInt()
                    } catch (_: NumberFormatException) {
                        0
                    }
                return parse()
            }
        """.trimIndent()

        val line = sut.lintWithContext(environment, code)
            .single()
            .entity
            .location
            .source
            .line

        assertEquals(1, line)
    }

    @Test
    fun `a try finally without a catch passes`() {
        val code = """
            fun open() {}
            fun close() {}

            fun load(): String {
                try {
                    open()
                    return ""
                } finally {
                    close()
                }
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `runCatching passes`() {
        val code = """
            fun parse(text: String): Int? =
                runCatching { text.toInt() }.getOrNull()
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a catch call on a flow passes`() {
        val code = """
            interface Flow<T>

            fun <T> Flow<T>.catch(action: (Throwable) -> Unit): Flow<T> = this

            fun numbers(source: Flow<Int>): Flow<Int> =
                source.catch { println(it) }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a function without a try passes`() {
        val code = """
            fun parse(text: String): Int? =
                text.toIntOrNull()
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a try in a property getter passes`() {
        val code = """
            class Parser(private val text: String) {
                val number: Int
                    get() = try {
                        text.toInt()
                    } catch (_: NumberFormatException) {
                        0
                    }
            }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an unresolved return type passes`() {
        val code = """
            fun parse(text: String): Unknown =
                try {
                    text.toInt()
                } catch (_: NumberFormatException) {
                    0
                }
        """.trimIndent()

        val findings = sut.lintWithContext(environment, code, allowCompilationErrors = true)

        assertEquals(0, findings.size)
    }
}
