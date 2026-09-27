package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import dev.detekt.test.utils.compileContentForTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GradleDeclarationOrderTest {

    private val sut = GradleDeclarationOrder(Config.empty)

    @Test
    fun `a sorted build script passes`() {
        val code = """
            plugins {
                id("kmp-di")
                id("kmp-lib")
                alias(libs.plugins.metro)
                alias(libs.plugins.sqldelight)
            }

            kotlin {
                sourceSets {
                    androidMain.dependencies {
                        implementation(libs.sqldelight.driver.android)
                    }
                    commonMain.dependencies {
                        implementation(projects.core.arch)
                        implementation(projects.core.otp)

                        implementation(libs.kotlinx.coroutines.core)
                        implementation(libs.sqldelight.coroutines.extensions)
                    }
                    commonTest.dependencies {
                        implementation(projects.core.test)

                        implementation(libs.kotlin.test)
                        implementation(libs.kotlinx.coroutines.test)
                    }
                    getByName("jvmTest").dependencies {
                        implementation(libs.kotlinx.coroutines.swing)
                    }
                }
            }
        """.trimIndent()

        val findings = sut.lint(compileContentForTest(code, "build.gradle.kts"))

        assertEquals(0, findings.size)
    }

    @Test
    fun `a kotlin file is ignored`() {
        val code = """
            fun plugins(block: () -> Unit) = block()
            fun alias(name: String) = name
            fun id(name: String) = name

            val applied = plugins {
                alias("metro")
                id("kmp-lib")
            }
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `an alias before an id is reported`() {
        val code = """
            plugins {
                alias(libs.plugins.metro)
                id("kmp-lib")
            }
        """.trimIndent()

        val findings = sut.lint(compileContentForTest(code, "build.gradle.kts"))

        assertEquals(1, findings.size)
    }

    @Test
    fun `unsorted ids are reported`() {
        val code = """
            plugins {
                id("static-analysis")
                id("code-coverage")
            }
        """.trimIndent()

        val findings = sut.lint(compileContentForTest(code, "build.gradle.kts"))

        assertEquals(1, findings.size)
    }

    @Test
    fun `unsorted aliases applied later are reported`() {
        val code = """
            plugins {
                alias(libs.plugins.metro) apply false
                alias(libs.plugins.kotlin.jvm) apply false
            }
        """.trimIndent()

        val findings = sut.lint(compileContentForTest(code, "build.gradle.kts"))

        assertEquals(1, findings.size)
    }

    @Test
    fun `unsorted source set dependency blocks are reported`() {
        val code = """
            kotlin {
                sourceSets {
                    commonMain.dependencies {
                        implementation(libs.decompose)
                    }
                    getByName("androidHostTest").dependencies {
                        implementation(libs.robolectric)
                    }
                }
            }
        """.trimIndent()

        val findings = sut.lint(compileContentForTest(code, "build.gradle.kts"))

        assertEquals(1, findings.size)
    }

    @Test
    fun `a library before a project is reported`() {
        val code = """
            dependencies {
                implementation(libs.decompose)
                implementation(projects.core.arch)
            }
        """.trimIndent()

        val findings = sut.lint(compileContentForTest(code, "build.gradle.kts"))

        assertEquals(1, findings.size)
    }

    @Test
    fun `a test library before a library is reported`() {
        val code = """
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.mvikotlin.main)
            }
        """.trimIndent()

        val findings = sut.lint(compileContentForTest(code, "build.gradle.kts"))

        assertEquals(1, findings.size)
    }

    @Test
    fun `unsorted libraries are reported`() {
        val code = """
            dependencies {
                implementation(libs.mvikotlin)
                implementation(libs.decompose)
            }
        """.trimIndent()

        val findings = sut.lint(compileContentForTest(code, "build.gradle.kts"))

        assertEquals(1, findings.size)
    }

    @Test
    fun `a split configuration is reported`() {
        val code = """
            dependencies {
                implementation(libs.decompose)
                debugImplementation(libs.compose.ui.tooling)
                implementation(libs.mvikotlin)
            }
        """.trimIndent()

        val findings = sut.lint(compileContentForTest(code, "build.gradle.kts"))

        assertEquals(1, findings.size)
    }

    @Test
    fun `configurations sorted each on their own pass`() {
        val code = """
            dependencies {
                implementation(projects.shared)
                implementation(libs.decompose)

                api(projects.feature.otp.tags)
                api(libs.decompose)

                androidTestImplementation(projects.core.uiTest)
                androidTestImplementation(libs.ultron.compose)
            }
        """.trimIndent()

        val findings = sut.lint(compileContentForTest(code, "build.gradle.kts"))

        assertEquals(0, findings.size)
    }

    @Test
    fun `the test libraries are configurable`() {
        val sut = GradleDeclarationOrder(TestConfig("testLibraries" to listOf("turbine")))
        val code = """
            dependencies {
                implementation(libs.turbine)
                implementation(libs.mvikotlin)
            }
        """.trimIndent()

        val findings = sut.lint(compileContentForTest(code, "build.gradle.kts"))

        assertEquals(1, findings.size)
    }
}
