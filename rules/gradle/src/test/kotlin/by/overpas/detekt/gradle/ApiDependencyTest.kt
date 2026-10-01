package by.overpas.detekt.gradle

import dev.detekt.api.Config
import dev.detekt.test.lint
import dev.detekt.test.utils.compileContentForTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiDependencyTest {

    private val sut = ApiDependency(Config.empty)

    @Test
    fun `an implementation dependency passes`() {
        val code = """
            dependencies {
                implementation(libs.decompose)
            }
        """.trimIndent()

        val findings = sut.lint(compileContentForTest(code, "build.gradle.kts"))

        assertEquals(0, findings.size)
    }

    @Test
    fun `an api dependency that is not exported is reported`() {
        val code = """
            dependencies {
                api(libs.kotlinx.coroutines.test)
            }
        """.trimIndent()

        val findings = sut.lint(compileContentForTest(code, "build.gradle.kts"))

        assertEquals(1, findings.size)
    }

    @Test
    fun `an exported api library passes`() {
        val code = """
            kotlin {
                iosArm64().binaries.framework {
                    export(libs.decompose)
                }
                sourceSets {
                    commonMain.dependencies {
                        api(libs.decompose)
                    }
                }
            }
        """.trimIndent()

        val findings = sut.lint(compileContentForTest(code, "build.gradle.kts"))

        assertEquals(0, findings.size)
    }

    @Test
    fun `an exported api project passes`() {
        val code = """
            kotlin {
                iosArm64().binaries.framework {
                    export(projects.feature.otp.tags)
                }
                sourceSets {
                    commonMain.dependencies {
                        api(projects.feature.otp.tags)
                    }
                }
            }
        """.trimIndent()

        val findings = sut.lint(compileContentForTest(code, "build.gradle.kts"))

        assertEquals(0, findings.size)
    }

    @Test
    fun `a source set api configuration is reported`() {
        val code = """
            dependencies {
                commonMainApi(projects.core.otp)
            }
        """.trimIndent()

        val findings = sut.lint(compileContentForTest(code, "build.gradle.kts"))

        assertEquals(1, findings.size)
    }

    @Test
    fun `a kotlin file is ignored`() {
        val code = """
            fun api(name: String) = name

            val dependency = api("decompose")
        """.trimIndent()

        val findings = sut.lint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `every unexported api dependency is reported`() {
        val code = """
            kotlin {
                iosArm64().binaries.framework {
                    export(libs.decompose)
                }
                sourceSets {
                    commonMain.dependencies {
                        api(projects.core.otp)
                        api(libs.decompose)
                        api(libs.mvikotlin)
                    }
                }
            }
        """.trimIndent()

        val findings = sut.lint(compileContentForTest(code, "build.gradle.kts"))

        assertEquals(2, findings.size)
    }
}
