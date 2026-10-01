# detekt-rules

Opinionated [detekt](https://detekt.dev) rules for Kotlin, Compose, coroutines, Gradle and tests.

| Module | Rule set id | Jar |
|---|---|---|
| `rules:architecture` | `overpas-architecture` | `detekt-rules-architecture-<version>.jar` |
| `rules:compose` | `overpas-compose` | `detekt-rules-compose-<version>.jar` |
| `rules:coroutines` | `overpas-coroutines` | `detekt-rules-coroutines-<version>.jar` |
| `rules:gradle` | `overpas-gradle` | `detekt-rules-gradle-<version>.jar` |
| `rules:style` | `overpas-style` | `detekt-rules-style-<version>.jar` |
| `rules:testing` | `overpas-testing` | `detekt-rules-testing-<version>.jar` |

## Build

```shell
./gradlew build      # compile, test, detekt
./gradlew koverVerify
./gradlew ruleJars   # collects the versioned jars into releases/<version>/
```

## Use in a project

Copy the jars of a release from `releases/<version>/` into the project, e.g. `config/detekt/plugins/`, and add them to the detekt plugins:

```kotlin
dependencies {
    detektPlugins(fileTree(rootProject.layout.projectDirectory.dir("config/detekt/plugins")) { include("*.jar") })
}
```

Configure the rules under their rule set ids in the detekt config. [Rules](#rules) lists the options
of each rule. `RepeatedCollaboratorType` needs type resolution, so configure it in the config of the
type resolution tasks (see `config/detekt/detekt-type-resolution.yml`).

To upgrade, delete the old jars in the same change, so that two versions are never on the plugin
classpath. Stop the Gradle daemon (`./gradlew --stop`) after a jar change, because the daemon caches
the old plugin classes.

## Rules

Every rule also takes the standard detekt
options `active`, `severity`, `excludes` and `includes`.

### `overpas-architecture`

#### `NonInjectedDependency`

A DI provider constructs only the dependency it provides. Inject every other dependency as a parameter of the provider.

| Option | Default | Description |
|---|---|---|
| `providerAnnotations` | `['Provides', 'Binds']` | Short names of the annotations that mark a DI provider |

Fails:

```kotlin
object Providers {
    @Provides
    fun repository(): Repository =
        DefaultRepository(api = HttpApi())
}

object Providers {
    @Provides
    fun queries(driver: SqlDriver): Queries =
        Database(driver).queries
}
```

Passes:

```kotlin
object Providers {
    @Provides
    fun repository(api: Api): Repository =
        DefaultRepository(api = api)
}
```

#### `RepeatedCollaboratorType`

A constructor must not take more than one collaborator of the same type. A group of same-type collaborators tends to grow, so the class does not scale. Put the group behind one abstraction. Needs type resolution: configure it in the config of the type resolution tasks.

| Option | Default | Description |
|---|---|---|
| `excludedTypes` | `['kotlin.Boolean', 'kotlin.Char', 'kotlin.Number', 'kotlin.UByte', 'kotlin.UShort', 'kotlin.UInt', 'kotlin.ULong', 'kotlin.CharSequence', 'kotlin.collections.Iterable', 'kotlin.collections.Map', 'kotlin.Array', 'kotlin.BooleanArray', 'kotlin.CharArray', 'kotlin.ByteArray', 'kotlin.ShortArray', 'kotlin.IntArray', 'kotlin.LongArray', 'kotlin.FloatArray', 'kotlin.DoubleArray', 'kotlin.sequences.Sequence', 'kotlin.Function', 'kotlin.Enum', 'kotlin.time.Duration', 'kotlin.time.Instant', 'kotlin.uuid.Uuid', 'kotlin.coroutines.CoroutineContext', 'kotlinx.coroutines.CoroutineDispatcher']` | Fully qualified names of the types that a constructor can take more than once, with their subtypes |
| `ignoredSupertypes` | `['kotlin.Any', 'kotlin.Comparable', 'java.io.Serializable']` | Fully qualified names of the supertypes that do not make two type arguments related |
| `areDataClassesIgnored` | `true` | Ignore the constructors of data classes |

Fails:

```kotlin
interface Repository
class Interactor(private val users: Repository, private val orders: Repository)

interface Entity
class User : Entity
class Order : Entity
interface Store<T>
class Interactor(users: Store<User>, orders: Store<Order>)
```

Passes:

```kotlin
interface Users
interface Orders
class Interactor(users: Users, orders: Orders)

class Account
data class Transfer(val from: Account, val to: Account)
```

### `overpas-compose`

#### `AnimatedContentTargetIgnored`

The outgoing and the incoming content are composed together. Content that ignores the target of its lambda shows the newest state in both.

| Option | Default | Description |
|---|---|---|
| `contentSwitchCalls` | `['AnimatedContent', 'Crossfade']` | Names of the calls whose content lambda receives the target state |

Fails:

```kotlin
@Composable
fun Screen(selectedId: String) {
    AnimatedContent(targetState = selectedId) {
        Destination(selectedId)
    }
}
```

Passes:

```kotlin
@Composable
fun Screen(selectedId: String) {
    AnimatedContent(targetState = selectedId) { targetId ->
        Destination(targetId)
    }
}
```

#### `CallerModifierNotFirst`

The caller owns the placement of a composable. Start the root modifier chain with the modifier parameter, then add the intrinsic modifiers.

| Option | Default | Description |
|---|---|---|
| `composableAnnotations` | `['Composable']` | Short names of the annotations that mark a composable function |
| `modifierName` | `'modifier'` | Name of the modifier parameter |

Fails:

```kotlin
@Composable
fun Avatar(url: String, modifier: Modifier = Modifier) {
    Image(
        modifier = Modifier
            .clip(CircleShape)
            .then(modifier),
    )
}
```

Passes:

```kotlin
@Composable
fun Avatar(url: String, modifier: Modifier = Modifier) {
    Image(
        modifier = modifier
            .clip(CircleShape)
            .size(48.dp),
    )
}
```

#### `FalseStabilityPromise`

A false stability promise lets Compose skip a recomposition and show stale UI. An immutable class has only read-only properties. A stable class changes only through snapshot state.

| Option | Default | Description |
|---|---|---|
| `immutableAnnotations` | `['Immutable']` | Short names of the annotations that promise immutability |
| `stableAnnotations` | `['Stable']` | Short names of the annotations that promise stability |
| `collectionTypes` | `['MutableList', 'MutableSet', 'MutableMap', 'MutableCollection', 'ArrayList', 'HashSet', 'HashMap', 'LinkedHashSet', 'LinkedHashMap']` | Short names of the mutable collection types |
| `collectionFactories` | `['mutableListOf', 'mutableSetOf', 'mutableMapOf', 'arrayListOf', 'hashSetOf', 'hashMapOf', 'linkedSetOf', 'linkedMapOf', 'toMutableList', 'toMutableSet', 'toMutableMap', 'ArrayList', 'HashSet', 'HashMap', 'LinkedHashSet', 'LinkedHashMap']` | Names of the calls that create a mutable collection |

Fails:

```kotlin
@Immutable
class State(var name: String)

@Stable
class State {
    var name = ""
}
```

Passes:

```kotlin
@Immutable
data class State(val name: String, val items: List<Item>)

@Stable
class State {
    var name by mutableStateOf("")
}
```

#### `ModifierChainWrapping`

A long modifier chain on one line is hard to scan. Put each call of the chain on a new line.

| Option | Default | Description |
|---|---|---|
| `chainStarts` | `['Modifier', 'modifier']` | Names of the expressions that start a modifier chain |
| `minCallCount` | `3` | Minimum number of calls in a chain that must be one per line |

Fails:

```kotlin
val modifier = Modifier.fillMaxWidth().padding(8.dp).clip(CircleShape)

val modifier = modifier.fillMaxWidth()
    .padding(8.dp)
    .clip(CircleShape)
```

Passes:

```kotlin
val modifier = Modifier
    .fillMaxWidth()
    .padding(8.dp)
    .clip(CircleShape)

val modifier = Modifier.fillMaxWidth().padding(8.dp)
```

#### `MutableCollectionInMutableState`

Compose does not see an in-place change of a mutable collection in a MutableState. Use mutableStateListOf or mutableStateMapOf, or put a read-only collection in the state.

| Option | Default | Description |
|---|---|---|
| `stateFactories` | `['mutableStateOf']` | Names of the calls that create a MutableState |
| `collectionFactories` | `['mutableListOf', 'mutableSetOf', 'mutableMapOf', 'arrayListOf', 'hashSetOf', 'hashMapOf', 'linkedSetOf', 'linkedMapOf', 'toMutableList', 'toMutableSet', 'toMutableMap', 'ArrayList', 'HashSet', 'HashMap', 'LinkedHashSet', 'LinkedHashMap']` | Names of the calls that create a mutable collection |
| `collectionTypes` | `['MutableList', 'MutableSet', 'MutableMap', 'MutableCollection', 'ArrayList', 'HashSet', 'HashMap', 'LinkedHashSet', 'LinkedHashMap']` | Short names of the mutable collection types |

Fails:

```kotlin
val items = mutableStateOf(mutableListOf<Item>())

val items = mutableStateOf<MutableList<Item>>(source)
```

Passes:

```kotlin
val items = mutableStateListOf<Item>()

val items = mutableStateOf(listOf<Item>())
```

#### `RequestFocusInComposition`

A focus request in the composable body runs on each recomposition. Request the focus from LaunchedEffect or from an event callback.

| Option | Default | Description |
|---|---|---|
| `composableAnnotations` | `['Composable']` | Short names of the annotations that mark a composable function |
| `effectCalls` | `['LaunchedEffect', 'DisposableEffect', 'SideEffect']` | Names of the composable calls whose trailing lambda is not composable content |

Fails:

```kotlin
@Composable
fun Screen(requester: FocusRequester) {
    requester.requestFocus()
    Button(modifier = Modifier.focusRequester(requester))
}
```

Passes:

```kotlin
@Composable
fun Screen(requester: FocusRequester) {
    LaunchedEffect(requester) {
        requester.requestFocus()
    }
    Button(modifier = Modifier.focusRequester(requester))
}
```

#### `ReturnInComposable`

A composable function that returns Unit must not contain return statements. Use if or when instead.

| Option | Default | Description |
|---|---|---|
| `composableAnnotations` | `['Composable']` | Short names of the annotations that mark a composable function |

Fails:

```kotlin
@Composable
fun Screen(state: State) {
    if (state.isLoading) return
    Content(state)
}
```

Passes:

```kotlin
@Composable
fun Screen(state: State) {
    if (state.isLoading) {
        Loader()
    } else {
        Content(state)
    }
}
```

### `overpas-coroutines`

#### `LaunchInInitializer`

A constructor can register a callback, but it must not launch a coroutine. Launch the work from an explicit start point or a lifecycle callback.

| Option | Default | Description |
|---|---|---|
| `launchCalls` | `['launch', 'async', 'launchIn']` | Names of the calls that launch a coroutine |

Fails:

```kotlin
class Component(scope: CoroutineScope) {
    init {
        scope.launch { load() }
    }
}

class Loader(scope: CoroutineScope) {
    private val data = scope.async { load() }
}
```

Passes:

```kotlin
class Component(private val scope: CoroutineScope) {
    fun start() {
        scope.launch { load() }
    }
}

class Component(scope: CoroutineScope, lifecycle: Lifecycle) {
    init {
        lifecycle.doOnCreate {
            scope.launch { load() }
        }
    }
}
```

#### `RunBlockingOutsideMain`

runBlocking blocks a thread and hides the cancellation. Use it only at a true blocking edge such as main. Use suspend functions or runTest instead.

| Option | Default | Description |
|---|---|---|
| `allowedFunctions` | `['main']` | Names of the top-level functions that can call runBlocking |

Fails:

```kotlin
fun load(): Data =
    runBlocking {
        repository.load()
    }

class App {
    fun main() {
        runBlocking { run() }
    }
}
```

Passes:

```kotlin
suspend fun load(): Data =
    repository.load()

fun main() {
    runBlocking {
        run()
    }
}
```

#### `SharingInFunction`

Each call of a function or a getter starts one more sharing coroutine. Expose stateIn and shareIn as one shared property.

| Option | Default | Description |
|---|---|---|
| `sharingCalls` | `['stateIn', 'shareIn', 'stateInComponent']` | Names of the calls that start a sharing coroutine |

Fails:

```kotlin
class Holder(private val scope: CoroutineScope, private val flow: Flow<Int>) {
    fun state(): StateFlow<Int> =
        flow.stateIn(scope, SharingStarted.Eagerly, 0)
}

class Holder(private val scope: CoroutineScope, private val flow: Flow<Int>) {
    val events: SharedFlow<Int>
        get() = flow.shareIn(scope, SharingStarted.Eagerly)
}
```

Passes:

```kotlin
class Holder(scope: CoroutineScope, flow: Flow<Int>) {
    val state: StateFlow<Int> = flow.stateIn(scope, SharingStarted.Eagerly, 0)
}
```

#### `StoredCoroutineScope`

A stored CoroutineScope hides who owns the work, and a cancelled scope silently drops later launches. Expose suspend functions and let the lifecycle owner launch them.

| Option | Default | Description |
|---|---|---|
| `scopeTypes` | `['CoroutineScope']` | Short names of the coroutine scope types |
| `scopeFactories` | `['CoroutineScope', 'MainScope']` | Names of the calls that create a coroutine scope |
| `allowedClasses` | `[]` | Short names of the classes that own a coroutine scope as lifecycle infrastructure |

Fails:

```kotlin
class Repository(private val scope: CoroutineScope)

class Repository {
    private val scope = CoroutineScope(Dispatchers.Default)
}
```

Passes:

```kotlin
class Repository(private val api: Api) {
    suspend fun load(): Data =
        api.load()
}

fun run() {
    val scope = CoroutineScope(Dispatchers.Default)
    scope.launch { work() }
}
```

### `overpas-gradle`

#### `ApiDependency`

An api() dependency leaks into every consumer. Only a dependency the script exports to the iOS framework needs it.

No options.

Fails:

```kotlin
dependencies {
    api(libs.decompose)
}

dependencies {
    commonMainApi(projects.core.otp)
}
```

Passes:

```kotlin
dependencies {
    implementation(libs.decompose)
}

kotlin {
    iosArm64().binaries.framework {
        export(libs.decompose)
    }
    sourceSets {
        commonMain.dependencies {
            api(libs.decompose)
        }
    }
}
```

#### `GradleDeclarationOrder`

Plugins, source set dependency blocks and dependencies of a build script are sorted, so a reader finds a declaration at once.

| Option | Default | Description |
|---|---|---|
| `testLibraries` | `['androidx.compose.ui.test', 'androidx.espresso', 'androidx.test', 'compose.ui.test', 'detekt.test', 'junit', 'kotlin.test', 'kotlinx.coroutines.test', 'robolectric', 'ultron']` | Version catalog aliases, after `libs.`, of the libraries only tests use |

Fails:

```kotlin
plugins {
    alias(libs.plugins.metro)
    id("kmp-lib")
}

dependencies {
    implementation(libs.kotlin.test)
    implementation(libs.decompose)
    implementation(projects.core.arch)
}
```

Passes:

```kotlin
plugins {
    id("kmp-lib")
    alias(libs.plugins.metro)
}

dependencies {
    implementation(projects.core.arch)
    implementation(libs.decompose)
    implementation(libs.kotlin.test)
}
```

### `overpas-style`

#### `ExpressionBodyOnNewLine`

An expression body on the signature line is hard to scan. Put a newline after `=`.

No options.

Fails:

```kotlin
fun answer(): Int = 42

fun sign(value: Int): String = when {
    value < 0 -> "-"
    else -> "+"
}
```

Passes:

```kotlin
fun answer(): Int =
    42

fun sum(
    first: Int,
    second: Int,
): Int =
    first + second
```

#### `FileStructure`

A file must follow the project layout: constants, properties, one interface, one class and functions. Many classes are allowed only when all of them extend the file interface. Class and interface bodies must follow the member order too.

No options.

Fails:

```kotlin
fun emptyBox(): Box =
    Box(0)

class Box(override val size: Int) : Container

interface Container {
    val size: Int
}

const val LIMIT = 1
```

Passes:

```kotlin
const val LIMIT = 1

interface Container {
    val size: Int
}

class Box(override val size: Int) : Container

fun emptyBox(): Box =
    Box(0)
```

#### `ForwardedParameter`

A function must not pass its own parameter to another function. Pass a primitive or an object that the function creates, or call the function on the parameter. Consider refactoring with an extension function, or even removing the function.

| Option | Default | Description |
|---|---|---|
| `allowedTypes` | `['Int', 'Long', 'Short', 'Byte', 'Float', 'Double', 'Boolean', 'Char', 'String', 'UInt', 'ULong', 'UShort', 'UByte']` | Short names of the parameter types that a function can pass on |
| `composableAnnotations` | `['Composable']` | Short names of the annotations that mark a composable function |
| `allowedCalls` | `['with', 'run', 'let', 'also', 'apply', 'takeIf', 'takeUnless', 'contract', 'callsInPlace', 'returns', 'returnsNotNull', 'implies', 'require', 'requireNotNull', 'check', 'checkNotNull', 'error', 'assert', 'listOf', 'listOfNotNull', 'mutableListOf', 'setOf', 'mutableSetOf', 'mapOf', 'mutableMapOf', 'arrayOf', 'sequenceOf', 'zip', 'plus', 'minus', 'contains', 'containsAll', 'containsKey', 'containsValue', 'indexOf', 'lastIndexOf', 'getOrElse', 'getOrDefault', 'getOrPut', 'union', 'intersect', 'subtract', 'add', 'addAll', 'remove', 'removeAll', 'retainAll', 'put', 'putAll', 'joinTo', 'toCollection']` | Names of the stdlib calls that can take a parameter |

Fails:

```kotlin
fun save(user: User) {
    repository.store(user)
}

fun listen(onEvent: (Event) -> Unit) {
    bus.subscribe(onEvent)
}
```

Passes:

```kotlin
fun save(user: User) {
    user.store()
}

fun load(id: Int, name: String?) {
    fetch(id, name)
}
```

#### `LinearContainsCheck`

A membership check on a list or an array runs in linear time. A Set or a Map finds a value in constant time.

| Option | Default | Description |
|---|---|---|
| `linearTypes` | `['List', 'MutableList', 'ArrayList', 'LinkedList', 'Array']` | Short names of the types that do not find a value by a hash |
| `linearFactories` | `['listOf', 'listOfNotNull', 'mutableListOf', 'arrayListOf', 'emptyList', 'buildList', 'arrayOf', 'emptyArray', 'arrayOfNulls', 'asList', 'toList', 'toMutableList', 'toTypedArray']` | Names of the calls that return a list or an array |

Fails:

```kotlin
fun known(id: Int): Boolean =
    id in listOf(1, 2, 3)

class Cache(private val ids: List<Int>) {
    fun holds(id: Int): Boolean =
        ids.contains(id)
}
```

Passes:

```kotlin
fun known(id: Int): Boolean =
    id in setOf(1, 2, 3)

class Cache(private val ids: Set<Int>) {
    fun holds(id: Int): Boolean =
        ids.contains(id)
}
```

#### `MutableVariable`

A mutable variable makes the state hard to follow. Use val instead.

| Option | Default | Description |
|---|---|---|
| `composableAnnotations` | `['Composable']` | Short names of the annotations that mark a composable function |

Fails:

```kotlin
fun count(): Int {
    var total = 0
    total += 1
    return total
}

class Counter {
    var value = 0
}
```

Passes:

```kotlin
fun count(): Int {
    val total = 0
    return total + 1
}

@Composable
fun Dropdown() {
    var expanded by remember { mutableStateOf(false) }
    Menu(expanded) { expanded = it }
}
```

#### `RedundantFunctionName`

A function name must not repeat the subject that the name of its class already gives. In `UserRepository`, `get` is enough and `getUser` is redundant.

| Option | Default | Description |
|---|---|---|
| `ignoredWords` | `['And', 'At', 'By', 'For', 'From', 'In', 'Of', 'On', 'Or', 'To', 'With']` | Words that a function name can repeat from the class name |

Fails:

```kotlin
interface UserRepository {
    fun getUser(id: String): User
    fun getUsers(): List<User>
}

interface OtpEntryRepository {
    fun getOtpEntry(id: String): OtpEntry
}
```

Passes:

```kotlin
interface UserRepository {
    fun get(id: String): User
    fun getAll(): List<User>
}
```

#### `SubjectlessWhenOnOneValue`

A subjectless when that tests one value in each branch hides the classified value. Use when with a subject instead.

No options.

Fails:

```kotlin
fun label(event: Event): String =
    when {
        event is Event.Message -> "message"
        event is Event.Empty -> "empty"
        else -> "other"
    }
```

Passes:

```kotlin
fun label(event: Event): String =
    when (event) {
        is Event.Message -> "message"
        is Event.Empty -> "empty"
        else -> "other"
    }
```

#### `TypeCast`

A type cast skips the compiler type check. Use a smart cast or a typed API instead.

No options.

Fails:

```kotlin
fun name(value: Any): String =
    value as String

fun name(value: Any): String? =
    value as? String
```

Passes:

```kotlin
fun length(value: Any): Int =
    if (value is String) value.length else 0
```

### `overpas-testing`

#### `ComplexAssertion`

A call or an object creation must not be embedded into an assertion. An assertion accepts variables and literals only.

| Option | Default | Description |
|---|---|---|
| `testAnnotations` | `['Test']` | Short names of the annotations that mark a unit test |
| `assertionPrefixes` | `['assert', 'verify', 'fail']` | Name prefixes of the calls that count as an assertion |

Fails:

```kotlin
@Test
fun `a test`() {
    val actual = sut.compute()

    assertEquals(expected, actual.toList())
}

@Test
fun `a test`() {
    val input = sut.sanitize("5")

    assertEquals(CounterInput.Accepted(5L), input)
}
```

Passes:

```kotlin
@Test
fun `a test`() {
    val expected = CounterInput.Accepted(5L)

    val input = sut.sanitize("5")

    assertEquals(expected, input)
}
```

#### `ExceptionMessageAssertion`

A unit test must not read the message of an exception. An exception message is not a contract.

| Option | Default | Description |
|---|---|---|
| `testAnnotations` | `['Test']` | Short names of the annotations that mark a unit test |
| `exceptionAccessors` | `['message', 'localizedMessage', 'stackTraceToString']` | Names of the exception accessors that a unit test must not read |

Fails:

```kotlin
@Test
fun `a test`() {
    val error = assertFails { sut.decode() }

    assertEquals("broken", error.message)
}
```

Passes:

```kotlin
@Test
fun `a test`() {
    val error = assertFails { sut.decode() }

    assertIs<IllegalStateException>(error)
}
```

#### `HelperFunctionInTest`

A test class and its file must hold no helper functions, only test lifecycle functions.

| Option | Default | Description |
|---|---|---|
| `testAnnotations` | `['Test']` | Short names of the annotations that mark a test |
| `lifecycleAnnotations` | `['Test', 'BeforeTest', 'AfterTest', 'Before', 'After', 'BeforeClass', 'AfterClass', 'BeforeEach', 'AfterEach', 'BeforeAll', 'AfterAll']` | Short names of the annotations that mark an allowed test lifecycle function |

Fails:

```kotlin
class StoreTest {
    private val sut = Store()

    @Test
    fun `a test`() {
        val input = input()

        val actual = sut.compute(input)

        assertEquals(2, actual)
    }

    private fun input(): Int = 1
}
```

Passes:

```kotlin
class StoreTest {
    private val sut = Store()

    @BeforeTest
    fun setUp() {
        sut.init()
    }

    @Test
    fun `a test`() {
        val actual = sut.compute()

        assertEquals(2, actual)
    }
}
```

#### `IncorrectUnitTestFormat`

A unit test body must be separated by empty lines into an optional arrange block, an act block and a final assert block.

| Option | Default | Description |
|---|---|---|
| `testAnnotations` | `['Test']` | Short names of the annotations that mark a unit test |
| `assertionPrefixes` | `['assert', 'verify', 'fail']` | Name prefixes of the calls that count as an assertion |

Fails:

```kotlin
@Test
fun `a test`() {
    val actual = sut.compute()
    assertEquals(2, actual)
}

@Test
fun `a test`() {
    sut.bringToFront()

    sut.pop()

    val actual = sut.active()

    assertEquals(1, actual)
}
```

Passes:

```kotlin
@Test
fun `a test`() {
    val actual = sut.compute()

    assertEquals(2, actual)
}

@Test
fun `a test`() = runTest {
    val actual = sut.load()

    assertNull(actual)
}
```

#### `MisplacedAssertion`

An assertion is only allowed in the final assert block of a unit test body. No other call is allowed in that block.

| Option | Default | Description |
|---|---|---|
| `testAnnotations` | `['Test']` | Short names of the annotations that mark a unit test |
| `assertionPrefixes` | `['assert', 'verify', 'fail']` | Name prefixes of the calls that count as an assertion |

Fails:

```kotlin
@Test
fun `a test`() {
    val actual = sut.compute()
    assertNotNull(actual)

    assertEquals(2, actual)
}

@Test
fun `a test`() {
    sut.save(note)

    val actual = sut.read()
    assertEquals(note, actual)
}
```

Passes:

```kotlin
@Test
fun `a test`() {
    sut.save(note)

    val actual = sut.read()

    assertEquals(note, actual)
}
```

#### `MissingSubjectUnderTest`

The subject of a unit test must be named after the configured subject name, and the act block must use it.

| Option | Default | Description |
|---|---|---|
| `testAnnotations` | `['Test']` | Short names of the annotations that mark a unit test |
| `assertionPrefixes` | `['assert', 'verify', 'fail']` | Name prefixes of the calls that count as an assertion |
| `subjectName` | `'sut'` | Name of the subject under test |

Fails:

```kotlin
@Test
fun `a test`() {
    val input = 1

    val actual = validator.validate(input)

    assertEquals(2, actual)
}

@Test
fun `a test`() {
    val sut = Validator()

    val actual = compute()

    assertEquals(2, actual)
}
```

Passes:

```kotlin
@Test
fun `a test`() {
    val input = 1

    val actual = sut.validate(input)

    assertEquals(2, actual)
}

@Test
fun `a test`() {
    val sut = Long::asByteArray

    val actual = 1L.sut()

    assertEquals(8, actual.size)
}
```

#### `MultipleAssertions`

The final assert block of a unit test must contain exactly one assertion. Group related checks with one assertOn call.

| Option | Default | Description |
|---|---|---|
| `testAnnotations` | `['Test']` | Short names of the annotations that mark a unit test |
| `assertionPrefixes` | `['assert', 'verify', 'fail']` | Name prefixes of the calls that count as an assertion |

Fails:

```kotlin
@Test
fun `a test`() {
    val actual = sut.compute()

    assertNotNull(actual)
    assertEquals(2, actual)
}
```

Passes:

```kotlin
@Test
fun `a test`() {
    val actual = sut.compute()

    assertOn(actual) {
        assertEquals(1, first)
        assertEquals(2, second)
    }
}
```

#### `PreviewInTest`

A preview class or function shows sample data for the IDE and can change at any time. Tests must use fakes that they own.

| Option | Default | Description |
|---|---|---|
| `namePattern` | `'[Pp]review'` | Regex that the name of a forbidden class or function contains |

Fails:

```kotlin
class OtpScreenTest {
    private val component = PreviewOtpComponent()
    private val state = previewState()
}
```

Passes:

```kotlin
class OtpScreenTest {
    private val component = FakeOtpComponent()
}
```
