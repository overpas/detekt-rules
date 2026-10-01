package by.overpas.detekt.coroutines

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

class CoroutinesRuleSetProvider : RuleSetProvider {

    override val ruleSetId = RuleSetId("overpas-coroutines")

    override fun instance() =
        RuleSet(
            ruleSetId,
            listOf(
                ::LaunchInInitializer,
                ::RunBlockingOutsideMain,
                ::SharingInFunction,
                ::StoredCoroutineScope,
            ),
        )
}
