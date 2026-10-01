# Changelog

All notable changes are documented in this file. The project uses
[Semantic Versioning](https://semver.org).

## Unreleased

- `overpas-coroutines`: `SharingInFunction` no longer treats `stateInComponent` as a sharing call by
  default. Add it to `sharingCalls` to keep the old behavior.
- `overpas-gradle`: `GradleDeclarationOrder` no longer treats `ultron` aliases as test libraries by
  default. Add `ultron` to `testLibraries` to keep the old behavior.
- `overpas-testing`: `MultipleAssertions` findings now suggest one expected value or one assertion
  call that groups the related checks.

## 0.1.0

First release.

- `overpas-architecture`: NonInjectedDependency, RepeatedCollaboratorType.
- `overpas-compose`: AnimatedContentTargetIgnored, CallerModifierNotFirst, FalseStabilityPromise,
  ModifierChainWrapping, MutableCollectionInMutableState, RequestFocusInComposition,
  ReturnInComposable.
- `overpas-coroutines`: LaunchInInitializer, RunBlockingOutsideMain, SharingInFunction,
  StoredCoroutineScope.
- `overpas-gradle`: ApiDependency, GradleDeclarationOrder.
- `overpas-style`: ExpressionBodyOnNewLine, FileStructure, ForwardedParameter, LinearContainsCheck,
  MutableVariable, RedundantFunctionName, SubjectlessWhenOnOneValue, TypeCast.
- `overpas-testing`: ComplexAssertion, ExceptionMessageAssertion, HelperFunctionInTest,
  IncorrectUnitTestFormat, MisplacedAssertion, MissingSubjectUnderTest, MultipleAssertions,
  PreviewInTest.
