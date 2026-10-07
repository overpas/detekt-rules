# Changelog

All notable changes are documented in this file. The project uses
[Semantic Versioning](https://semver.org).

## 0.3.0

### Added rules

- `overpas-architecture`: `HiddenAbstraction` reports a class, an interface, an object or the top
  level of a file that has more helper functions than entry points. Put the helpers behind a new
  abstraction, or move them to the types that they use. The rule needs type resolution, so configure
  it in the config of the type resolution tasks.

## 0.2.0

### Added rules

- `overpas-compose`: `LowLevelUiPrimitive` reports imports and fully qualified names of low-level
  UI primitives in UI code: colors, typography, dimensions, shapes, drawing, custom layouts and
  animation specs. Use design system components and tokens instead. Set `forbiddenImports` and
  `allowedImports` to change the list of primitives, and the standard `includes` option to check
  only feature code.

## 0.1.1

### Changed options and defaults

- `overpas-coroutines`: `SharingInFunction` no longer treats `stateInComponent` as a sharing call by
  default. Add it to `sharingCalls` to keep the old behavior.
- `overpas-gradle`: `GradleDeclarationOrder` no longer treats `ultron` aliases as test libraries by
  default. Add `ultron` to `testLibraries` to keep the old behavior.

### Changed messages

- `overpas-testing`: `MultipleAssertions` findings now suggest one expected value or one assertion
  call that groups the related checks.

### Distribution

- The rule set jars are available only as assets of the GitHub releases. The repository no longer
  has a `releases/` folder.

## 0.1.0

First release.

### Added rules

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
