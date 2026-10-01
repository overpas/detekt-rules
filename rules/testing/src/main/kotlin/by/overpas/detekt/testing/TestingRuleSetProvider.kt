package by.overpas.detekt.testing

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

class TestingRuleSetProvider : RuleSetProvider {

    override val ruleSetId = RuleSetId("overpas-testing")

    override fun instance() =
        RuleSet(
            ruleSetId,
            listOf(
                ::ComplexAssertion,
                ::ExceptionMessageAssertion,
                ::HelperFunctionInTest,
                ::IncorrectUnitTestFormat,
                ::MisplacedAssertion,
                ::MissingSubjectUnderTest,
                ::MultipleAssertions,
                ::PreviewInTest,
            ),
        )
}
