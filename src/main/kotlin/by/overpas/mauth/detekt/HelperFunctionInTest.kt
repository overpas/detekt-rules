package by.overpas.mauth.detekt

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

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)
        if (!file.anyDescendantOfType<KtNamedFunction> { it.isUnitTest(testAnnotations) }) return
        file.declarations.filterIsInstance<KtNamedFunction>().forEach(::check)
        file.collectDescendantsOfType<KtClassOrObject> { it.isTestClass() }
            .flatMap { it.declarations + it.companionObjects.flatMap { companion -> companion.declarations } }
            .filterIsInstance<KtNamedFunction>()
            .forEach(::check)
    }

    private fun check(function: KtNamedFunction) {
        if (!function.isUnitTest(lifecycleAnnotations)) function.reportHelper()
        function.collectDescendantsOfType<KtNamedFunction> { it.isLocal && it.name != null }
            .forEach { it.reportHelper() }
    }

    private fun KtClassOrObject.isTestClass(): Boolean = declarations
        .filterIsInstance<KtNamedFunction>()
        .any { it.isUnitTest(testAnnotations) }

    private fun KtNamedFunction.reportHelper() {
        report(Finding(Entity.atName(this), "Move the helper function `$nameAsSafeName` out of the test file."))
    }
}
