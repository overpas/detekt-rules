package by.overpas.mauth.detekt

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

class MauthRuleSetProvider : RuleSetProvider {

    override val ruleSetId = RuleSetId("mauth")

    override fun instance() =
        RuleSet(
            ruleSetId,
            listOf(
                ::AnimatedContentTargetIgnored,
                ::ComplexAssertion,
                ::ExceptionMessageAssertion,
                ::ExpressionBodyOnNewLine,
                ::FileStructure,
                ::HelperFunctionInTest,
                ::IncorrectUnitTestFormat,
                ::LaunchInInitializer,
                ::LinearContainsCheck,
                ::MisplacedAssertion,
                ::MissingSubjectUnderTest,
                ::MultipleAssertions,
                ::MutableCollectionInMutableState,
                ::MutableVariable,
                ::RequestFocusInComposition,
                ::ReturnInComposable,
                ::RunBlockingOutsideMain,
                ::SharingInFunction,
                ::SubjectlessWhenOnOneValue,
            ),
        )
}
