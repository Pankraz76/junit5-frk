# junit-random-seed

Reproducible randomness for JUnit Jupiter 6. One logged root seed per test run reproduces
every random value used by every test.

This covers the use case of
[junit-team/junit-framework#5971](https://github.com/junit-team/junit-framework/issues/5971)
as a standalone extension, which is the direction the JUnit maintainers suggested.

## Usage

```java
@RandomSeeded
class ShuffleTests {

	@Test
	void shuffleKeepsElements(Random random) { ... }          // java.util.Random

	@Test
	void splits(RandomGenerator generator) { ... }            // SplittableRandom

	@Test
	void ownRng(@Seed long seed) { var rng = new MyRng(seed); ... }

	@ParameterizedTest
	@RandomNumberSource(count = 100, min = 0, max = 1_000)  // int, long, double (+ wrappers)
	void roundTrips(int value, Random random) { ... }
}
```

`@RandomNumberSource` registers the extension on its own; `@RandomSeeded` is not needed for it.

## Reproducing a failure

At `CONFIG` level the root seed is logged once per launcher session:

```
Using random root seed [8231946125573]. To reproduce this run, pass -Drandomseed.seed=8231946125573 ...
```

A failing test that used randomness logs a `WARNING`:

```
Test [shuffleKeepsElements(Random)] failed after using random seed(s) [-349527845255254238]
derived from root seed [42]. To reproduce, pass -Drandomseed.seed=42. Unique id: [engine:junit-jupiter]/...
```

Every derived seed is also published as report entries `randomseed.seed` and `randomseed.root`
(for example in the Open Test Reporting XML).

### Root seed precedence

1. `randomseed.seed`
2. `junit.jupiter.execution.order.random.seed`. Setting the Jupiter orderer seed reproduces
   both the random test order and the random data.
3. `System.nanoTime()`

Both are regular JUnit configuration parameters, so `-D`, `junit-platform.properties` and the
build-tool `configurationParameters` all work. An invalid value logs a warning and falls through
to the next source.

## How seeds are derived

`seed = splitmix64(root XOR splitmix64(fnv1a64(uniqueId ∘ 0x00 ∘ salt)))`, where the salt is the
declaring executable plus the parameter index, or a fixed value for `@RandomNumberSource`.

- **Stable:** the seed depends only on the root seed and the Jupiter unique id (class, method and
  invocation index), so it is the same in every run with the same root seed.
- **Independent:** every test, parameterized or repeated invocation, lifecycle method and
  injected parameter gets its own seed. Two `Random` parameters never produce the same sequence.
- **Fully mixed:** the SplitMix64 finalizer is a bijection with full avalanche, so `nanoTime()`
  seeds with weak low bits are not a problem.

## Limitations

- When no seed is configured, Jupiter's random orderers and this extension each generate their
  own `nanoTime()` seed, because JUnit does not expose its default seed (proposed in junit-team/junit-framework#5971). To
  get one value for both, set `junit.jupiter.execution.order.random.seed`.
- Renaming a test class or method, or reordering parameterized arguments, changes the derived
  seeds. The root seed still identifies the run.
- Once JUnit ships a platform-wide seed (for example `junit.platform.random.seed`, as proposed in
  junit-team/junit-framework#5971), it should be added to the precedence list.

## Build

```bash
gradle build
```

Requires JDK 17+. Depends only on `junit-jupiter-api` and `junit-jupiter-params` 6.1.3.

## Traceability

Scenarios: [docs/scenarios.md](docs/scenarios.md). Result: 29/29 passing.

| Scenario | Test |
|---|---|
| Derivation deterministic | `SeedMixerTests.deterministic` |
| Distinct ids / salts | `SeedMixerTests.distinct`, `saltBoundaryIsUnambiguous` |
| Weak root seeds mixed | `SeedMixerTests.avalanche` |
| Explicit seed wins | `RootSeedTests.explicitSeedWins` |
| Reuses orderer seed | `RootSeedTests.reusesOrdererSeed` |
| Generated seed shared, logged once | `RootSeedTests.generatedSeedIsSharedAndLoggedOnce` |
| Invalid seed → warn + fallback | `RootSeedTests.invalidSeedFallsBack` |
| Random / RandomGenerator / @Seed reproducible | `ParameterInjectionTests.injectsSupportedTypesReproducibly`, `differentRootSeedChangesValues`, `seedMatchesReportedSeed` |
| Parameters independent | `ParameterInjectionTests.parametersAreIndependent`, `lifecycleMethodsAreIndependent` |
| Invocations distinct, coexist with arguments | `ParameterInjectionTests.invocationsAreDistinct` |
| Unsupported parameters ignored | `ParameterInjectionTests.unsupportedParametersAreIgnored` |
| Source: declared type, count | `RandomNumberSourceTests.Valid.generatesDeclaredType`, `defaultCount` |
| Source: range | `RandomNumberSourceTests.Valid.respectsRange`, `longRangeKeepsFullPrecision` |
| Source: reproducible | `RandomNumberSourceTests.Valid.reproducible` |
| Source: coexists with injection | `RandomNumberSourceTests.Valid.coexistsWithInjection` |
| Source: invalid declarations | `RandomNumberSourceTests.Invalid.*` (5 tests) |
| Failure reported with reproduce hint | `FailureReportingTests.failingSeededTestIsReported`, `failingSourceInvocationIsReported` |
| No report without randomness | `FailureReportingTests.failingUnseededTestIsNotReported` |
