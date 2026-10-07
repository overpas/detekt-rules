package by.overpas.detekt.gradle

import by.overpas.detekt.core.calleeName
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
        val declarations = file.collectDescendantsOfType<KtCallExpression>().map(::Declaration)
        val exported = declarations
            .filter { it.isExport() }
            .map { it.notation() }
            .toSet()
        declarations
            .filter { it.isApi() && it.notation() !in exported }
            .forEach { report(Finding(Entity.from(it.call), it.findingMessage())) }
    }

    private fun Declaration.findingMessage(): String =
        "Use `implementation()` for `${notation()}`. " +
            "`api()` is only for a dependency this script exports to the iOS framework."

    private class Declaration(val call: KtCallExpression) {

        fun isExport(): Boolean =
            call.calleeName() == EXPORT

        fun isApi(): Boolean {
            val name = call.calleeName()
            return name == API || name?.endsWith(API_SUFFIX) == true
        }

        fun notation(): String =
            call.valueArguments
                .singleOrNull()
                ?.getArgumentExpression()
                ?.text
                .orEmpty()
    }
}
