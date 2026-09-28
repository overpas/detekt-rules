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
                ::ApiDependency,
                ::CallerModifierNotFirst,
                ::ComplexAssertion,
                ::ExceptionMessageAssertion,
                ::ExpressionBodyOnNewLine,
                ::FalseStabilityPromise,
                ::FileStructure,
                ::ForwardedParameter,
                ::GradleDeclarationOrder,
                ::HelperFunctionInTest,
                ::IncorrectUnitTestFormat,
                ::LaunchInInitializer,
                ::LinearContainsCheck,
                ::MisplacedAssertion,
                ::MissingSubjectUnderTest,
                ::ModifierChainWrapping,
                ::MultipleAssertions,
                ::MutableCollectionInMutableState,
                ::MutableVariable,
                ::PreviewInTest,
                ::RedundantFunctionName,
                ::RequestFocusInComposition,
                ::ReturnInComposable,
                ::RunBlockingOutsideMain,
                ::SharingInFunction,
                ::StoredCoroutineScope,
                ::SubjectlessWhenOnOneValue,
                ::TypeCast,
            ),
        )
}
