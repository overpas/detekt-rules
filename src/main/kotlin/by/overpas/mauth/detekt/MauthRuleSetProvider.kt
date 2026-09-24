package by.overpas.mauth.detekt

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

class MauthRuleSetProvider : RuleSetProvider {

    override val ruleSetId = RuleSetId("mauth")

    override fun instance() = RuleSet(
        ruleSetId,
        listOf(
            ::ComplexAssertion,
            ::ExceptionMessageAssertion,
            ::FileStructure,
            ::HelperFunctionInTest,
            ::IncorrectUnitTestFormat,
            ::LinearContainsCheck,
            ::MisplacedAssertion,
            ::MissingSubjectUnderTest,
            ::MultipleAssertions,
            ::MutableVariable,
            ::ReturnInComposable,
        ),
    )
}
