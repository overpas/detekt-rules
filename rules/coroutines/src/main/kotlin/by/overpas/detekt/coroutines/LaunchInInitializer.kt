package by.overpas.detekt.coroutines

import by.overpas.detekt.core.calleeName
import by.overpas.detekt.core.isDeferredWithin
import com.intellij.psi.PsiElement
import dev.detekt.api.Config
import dev.detekt.api.Configuration
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

class LaunchInInitializer(config: Config) :
    Rule(
        config,
        "A constructor can register a callback, but it must not launch a coroutine. " +
            "Launch the work from an explicit start point or a lifecycle callback.",
    ) {

    @Configuration("names of the calls that launch a coroutine")
    private val launchCalls: Set<String> by config(listOf("launch", "async", "launchIn")) { it.toSet() }

    override fun visitAnonymousInitializer(initializer: KtAnonymousInitializer) {
        super.visitAnonymousInitializer(initializer)
        initializer.body?.let { initializer.reportLaunches(scope = it) }
    }

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)
        if (property.isLocal) return
        property.initializer?.let { property.reportLaunches(scope = it) }
    }

    private fun PsiElement.reportLaunches(scope: PsiElement) {
        scope
            .collectDescendantsOfType<KtCallExpression> { !it.isDeferredWithin(this) }
            .forEach { call ->
                call.calleeName()
                    ?.takeIf { it in launchCalls }
                    ?.let { name -> call.reportLaunch(name) }
            }
    }

    private fun KtCallExpression.reportLaunch(name: String) {
        report(
            Finding(
                Entity.from(this),
                "Move `$name` out of the initializer. Launch it from an explicit start point or a lifecycle callback.",
            ),
        )
    }
}
