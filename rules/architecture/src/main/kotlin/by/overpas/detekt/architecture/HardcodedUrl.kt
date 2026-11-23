package by.overpas.detekt.architecture

import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

class HardcodedUrl(config: Config) :
    Rule(
        config,
        "Production code must not hardcode an HTTP or HTTPS URL outside the DI packages. " +
            "Supply the URL through DI, a build property or a resource.",
    ) {

    @Configuration("names of the package segments where a URL can be hardcoded")
    private val allowedPackageSegments: Set<String> by config(listOf("di")) { it.toSet() }

    @Configuration("prefixes of the URLs that can be hardcoded in any package, such as XML namespaces")
    private val allowedUrlPrefixes: List<String> by config(
        listOf("http://schemas.android.com/", "http://www.w3.org/"),
    )

    override fun visitStringTemplateExpression(expression: KtStringTemplateExpression) {
        super.visitStringTemplateExpression(expression)
        val segments = expression.containingKtFile.packageFqName.pathSegments().map { it.asString() }
        val hasUrl = expression.entries
            .filterIsInstance<KtLiteralStringTemplateEntry>()
            .any { entry ->
                URL.findAll(entry.text).any { url ->
                    allowedUrlPrefixes.none { entry.text.startsWith(it, url.range.first, ignoreCase = true) }
                }
            }
        if (hasUrl && segments.none { it in allowedPackageSegments }) {
            report(
                Finding(
                    Entity.from(expression),
                    "Do not hardcode the URL ${expression.text}. Supply it through DI, a build property or a resource.",
                ),
            )
        }
    }

    private companion object {
        val URL = Regex("https?://", RegexOption.IGNORE_CASE)
    }
}
