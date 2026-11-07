# Behaviour scenarios

Each scenario is covered by at least one test (see the traceability table in the README).

## Seed derivation

```gherkin
Scenario: Derivation is deterministic
  Given a root seed and a test unique id and a salt
  When the per-use seed is derived twice
  Then both results are equal

Scenario: Different tests, invocations and parameters get different seeds
  Given one root seed
  When seeds are derived for different unique ids or different salts
  Then all derived seeds differ

Scenario: Weak root seeds are still mixed across all bits
  Given root seeds that differ only in a single low bit (e.g. nanoTime ending in zeros)
  When per-use seeds are derived for the same unique id
  Then the results differ in roughly half of their 64 bits
```

## Root seed

```gherkin
Scenario: Explicit seed wins
  Given configuration parameter "randomseed.seed" = 42
  And "junit.jupiter.execution.order.random.seed" = 7
  When a seeded test runs
  Then root seed 42 is used

Scenario: Reuses the Jupiter random-orderer seed
  Given only "junit.jupiter.execution.order.random.seed" = 7
  When a seeded test runs
  Then root seed 7 is used, so one value reproduces ordering and data

Scenario: No configuration
  Given no seed configuration
  When several seeded tests run in one launcher session
  Then a single generated root seed is used for all of them
  And it is logged exactly once with a reproduce hint

Scenario: Invalid configuration
  Given "randomseed.seed" = "not-a-number"
  When a seeded test runs
  Then a warning naming the parameter and value is logged
  And a generated root seed is used
```

## Parameter injection

```gherkin
Scenario: Injects Random, RandomGenerator and @Seed long
  Given a @RandomSeeded test declaring Random, RandomGenerator and @Seed long parameters
  When it runs twice with the same root seed
  Then it receives identical values both times

Scenario: Each injected parameter is independent
  Given a test with two Random parameters
  Then they are seeded differently

Scenario: Each parameterized / repeated invocation is distinct
  Given a @ParameterizedTest or @RepeatedTest that also injects Random
  Then every invocation gets its own seed
  And injection coexists with the regular arguments

Scenario: Unsupported parameters are left alone
  Given a long parameter without @Seed
  Then the extension does not claim it
```

## @RandomNumberSource

```gherkin
Scenario: Generates the requested number of values of the declared type
  Given @RandomNumberSource(count = 5) on a test with an int / long / double parameter
  Then 5 invocations run with values of that type

Scenario: Respects the range
  Given min = 10 and max = 20
  Then every value v satisfies 10 <= v < 20

Scenario: Reproducible
  Given the same root seed
  Then the same values are generated

Scenario: Invalid declarations fail the test template
  Given count < 1, or min >= max, or an unsupported parameter type, or an int range outside int
  Then the container fails with a descriptive PreconditionViolationException
```

## Failure reporting

```gherkin
Scenario: A failing test that used randomness reports how to reproduce
  Given a seeded test that fails
  Then a warning is logged naming the test, its derived seed and the root seed
  And the hint "-Drandomseed.seed=<root>" is included

Scenario: A failing test that used no randomness is not reported

Scenario: Each seeded test publishes a report entry "randomseed.seed"
```
