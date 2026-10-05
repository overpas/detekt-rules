package by.overpas.detekt.compose

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

class ComposeRuleSetProvider : RuleSetProvider {

    override val ruleSetId = RuleSetId("overpas-compose")

    override fun instance() =
        RuleSet(
            ruleSetId,
            listOf(
                ::AnimatedContentTargetIgnored,
                ::CallerModifierNotFirst,
                ::FalseStabilityPromise,
                ::LowLevelUiPrimitive,
                ::ModifierChainWrapping,
                ::MutableCollectionInMutableState,
                ::RequestFocusInComposition,
                ::ReturnInComposable,
            ),
        )
}
