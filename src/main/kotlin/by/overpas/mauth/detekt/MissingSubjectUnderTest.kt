package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType

class MissingSubjectUnderTest(config: Config) :
    Rule(
        config,
        "The subject of a unit test must be named after the configured subject name, " +
            "and the act block must use it.",
    ) {

    @Configuration("short names of the annotations that mark a unit test")
    private val testAnnotations: Set<String> by config(listOf("Test")) { it.toSet() }

    @Configuration("name prefixes of the calls that count as an assertion")
    private val assertionPrefixes: List<String> by config(listOf("assert", "verify", "fail"))

    @Configuration("name of the subject under test")
    private val subjectName: String by config("sut")

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        if (!function.isUnitTest(testAnnotations)) return
        val blocks = function.testBody(assertionPrefixes)
            ?.blocks()
            ?.takeIf { it.size in MIN_BLOCK_COUNT..MAX_BLOCK_COUNT }
            ?: return
        val act = blocks[blocks.size - MIN_BLOCK_COUNT]
        if (act.none { it.usesSubject() }) {
            report(Finding(Entity.atName(function), "The act block must use `$subjectName`."))
        }
    }

    private fun KtExpression.usesSubject(): Boolean = isSubject() ||
        anyDescendantOfType<KtNameReferenceExpression> { it.isSubject() }

    private fun KtExpression.isSubject(): Boolean =
        this is KtNameReferenceExpression && getReferencedName() == subjectName
}
