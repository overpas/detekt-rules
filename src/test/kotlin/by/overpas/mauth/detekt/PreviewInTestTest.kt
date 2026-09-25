package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

class PreviewInTestTest {

    private val sut = PreviewInTest(Config.empty)

    @Test
    fun `a fake passes`() {
        val code = """
            class T {
                private val component = FakeOtpComponent()
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an import of a preview class passes`() {
        val code = """
            package a.preview

            import b.PreviewOtpComponent
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a string that contains preview passes`() {
        val code = """
            val text = "Preview"
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `a preview class constructor call is reported`() {
        val code = """
            class T {
                private val component = PreviewOtpComponent()
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a preview class type is reported`() {
        val code = """
            class T {
                private lateinit var component: PreviewOtpComponent
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a preview function call is reported`() {
        val code = """
            class T {
                private val state = previewState()
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a qualified preview object is reported`() {
        val code = """
            class T {
                private val state = Samples.Preview.state
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `a preview function reference is reported`() {
        val code = """
            class T {
                private val factory = ::previewState
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `the name pattern is configurable`() {
        val sut = PreviewInTest(TestConfig("namePattern" to "Sample"))
        val code = """
            class T {
                private val component = SampleOtpComponent()
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(1, findings.size)
    }
}
