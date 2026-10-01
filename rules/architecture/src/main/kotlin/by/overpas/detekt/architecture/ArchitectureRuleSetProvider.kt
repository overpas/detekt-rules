package by.overpas.detekt.architecture

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

class ArchitectureRuleSetProvider : RuleSetProvider {

    override val ruleSetId = RuleSetId("overpas-architecture")

    override fun instance() =
        RuleSet(
            ruleSetId,
            listOf(
                ::NonInjectedDependency,
                ::RepeatedCollaboratorType,
            ),
        )
}
