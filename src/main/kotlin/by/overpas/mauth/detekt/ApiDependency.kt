package by.overpas.mauth.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

private const val GRADLE_SCRIPT_SUFFIX = ".gradle.kts"
private const val EXPORT = "export"
private const val API = "api"
private const val API_SUFFIX = "Api"

class ApiDependency(config: Config) :
    Rule(
        config,
        "An api() dependency leaks into every consumer. Only a dependency the script exports " +
            "to the iOS framework needs it.",
    ) {

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)
        if (!file.name.endsWith(GRADLE_SCRIPT_SUFFIX)) return
        val calls = file.collectDescendantsOfType<KtCallExpression>()
        val exported = calls
            .filter { it.calleeName() == EXPORT }
            .map { it.dependencyNotation() }
            .toSet()
        calls
            .filter { it.isApiDeclaration() && it.dependencyNotation() !in exported }
            .forEach { report(Finding(Entity.from(it), it.findingMessage())) }
    }

    private fun KtCallExpression.isApiDeclaration(): Boolean {
        val name = calleeName()
        return name == API || name?.endsWith(API_SUFFIX) == true
    }

    private fun KtCallExpression.findingMessage(): String =
        "Use `implementation()` for `${dependencyNotation()}`. " +
            "`api()` is only for a dependency this script exports to the iOS framework."
}

private fun KtCallExpression.dependencyNotation(): String =
    valueArguments
        .singleOrNull()
        ?.getArgumentExpression()
        ?.text
        .orEmpty()
