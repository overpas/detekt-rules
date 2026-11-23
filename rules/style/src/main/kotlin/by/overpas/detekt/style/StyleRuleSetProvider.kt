package by.overpas.detekt.style

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

class StyleRuleSetProvider : RuleSetProvider {

    override val ruleSetId = RuleSetId("overpas-style")

    override fun instance() =
        RuleSet(
            ruleSetId,
            listOf(
                ::CollaboratorNameMismatch,
                ::ConversionNameMismatch,
                ::ExpressionBodyOnNewLine,
                ::FileStructure,
                ::ForwardedParameter,
                ::LinearContainsCheck,
                ::MutableVariable,
                ::RedundantFunctionName,
                ::RepeatedLazyValue,
                ::SetupWithoutApply,
                ::SubjectlessWhenOnOneValue,
                ::TypeCast,
            ),
        )
}
