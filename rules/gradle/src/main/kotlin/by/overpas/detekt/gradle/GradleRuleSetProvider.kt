package by.overpas.detekt.gradle

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

class GradleRuleSetProvider : RuleSetProvider {

    override val ruleSetId = RuleSetId("overpas-gradle")

    override fun instance() =
        RuleSet(
            ruleSetId,
            listOf(
                ::ApiDependency,
                ::GradleDeclarationOrder,
            ),
        )
}
