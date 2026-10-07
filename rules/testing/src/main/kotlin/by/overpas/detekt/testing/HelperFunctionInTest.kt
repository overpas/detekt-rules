package by.overpas.detekt.testing

import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

class HelperFunctionInTest(config: Config) :
    Rule(
        config,
        "A test class and its file must hold no helper functions, only test lifecycle functions.",
    ) {

    @Configuration("short names of the annotations that mark a test")
    private val testAnnotations: Set<String> by config(listOf("Test")) { it.toSet() }

    @Configuration("short names of the annotations that mark an allowed test lifecycle function")
    private val lifecycleAnnotations: Set<String> by config(
        listOf(
            "Test",
            "BeforeTest",
            "AfterTest",
            "Before",
            "After",
            "BeforeClass",
            "AfterClass",
            "BeforeEach",
            "AfterEach",
            "BeforeAll",
            "AfterAll",
        ),
    ) { it.toSet() }

    private val functions by lazy { TestFileFunctions(testAnnotations, lifecycleAnnotations) }

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)
        if (!file.anyDescendantOfType<KtNamedFunction> { it.isUnitTest(testAnnotations) }) return
        file.declarations
            .filterIsInstance<KtNamedFunction>()
            .forEach { it.reportHelpers() }
        file.collectDescendantsOfType<KtClassOrObject> { functions.isTestClass(it) }
            .flatMap { it.declarations + it.companionObjects.flatMap { companion -> companion.declarations } }
            .filterIsInstance<KtNamedFunction>()
            .forEach { it.reportHelpers() }
    }

    private fun KtNamedFunction.reportHelpers() {
        functions.helpers(this).forEach { helper ->
            report(
                Finding(
                    Entity.atName(helper),
                    "Move the helper function `${helper.nameAsSafeName}` out of the test file.",
                ),
            )
        }
    }

    private class TestFileFunctions(
        private val testAnnotations: Set<String>,
        private val lifecycleAnnotations: Set<String>,
    ) {

        fun isTestClass(classOrObject: KtClassOrObject): Boolean =
            classOrObject.declarations
                .filterIsInstance<KtNamedFunction>()
                .any { it.isUnitTest(testAnnotations) }

        fun helpers(function: KtNamedFunction): List<KtNamedFunction> =
            listOfNotNull(function.takeUnless { it.isUnitTest(lifecycleAnnotations) }) +
                function.collectDescendantsOfType<KtNamedFunction> { it.isLocal && it.name != null }
    }
}
