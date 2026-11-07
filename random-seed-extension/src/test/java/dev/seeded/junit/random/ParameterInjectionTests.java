package dev.seeded.junit.random;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.testkit.engine.EngineExecutionResults;

@DisplayName("Parameter injection")
class ParameterInjectionTests {

	private static final Map<String, String> SEED_42 = Map.of(RootSeed.SEED_PROPERTY_NAME, "42");

	@Test
	@DisplayName("given Random, RandomGenerator and @Seed long/Long when run twice with the same root seed then values are identical")
	void injectsSupportedTypesReproducibly() {
		var first = values(TestKit.run(AllTypesTestCase.class, SEED_42));
		var second = values(TestKit.run(AllTypesTestCase.class, SEED_42));

		assertThat(first).hasSize(4).doesNotHaveDuplicates().isEqualTo(second);
	}

	@Test
	@DisplayName("given a different root seed when run then values differ")
	void differentRootSeedChangesValues() {
		var first = values(TestKit.run(AllTypesTestCase.class, SEED_42));
		var second = values(TestKit.run(AllTypesTestCase.class, Map.of(RootSeed.SEED_PROPERTY_NAME, "43")));

		assertThat(first).doesNotContainAnyElementsOf(second);
	}

	@Test
	@DisplayName("given @Seed long when run then it equals the published derived seed")
	void seedMatchesReportedSeed() {
		var results = TestKit.run(SeedOnlyTestCase.class, SEED_42);

		results.testEvents().assertStatistics(stats -> stats.succeeded(1));
		assertThat(TestKit.reportValues(results, "value")).isEqualTo(TestKit.reportValues(results, Seeds.SEED_REPORT_KEY));
	}

	@Test
	@DisplayName("given two Random parameters when run then they are seeded independently")
	void parametersAreIndependent() {
		var values = values(TestKit.run(TwoRandomsTestCase.class, SEED_42));

		assertThat(values).hasSize(2).doesNotHaveDuplicates();
	}

	@Test
	@DisplayName("given Random in @BeforeEach and in the test method when run then they are seeded independently")
	void lifecycleMethodsAreIndependent() {
		var values = values(TestKit.run(LifecycleTestCase.class, SEED_42));

		assertThat(values).hasSize(2).doesNotHaveDuplicates();
	}

	@Test
	@DisplayName("given @ParameterizedTest and @RepeatedTest when run then each invocation gets its own seed and arguments still resolve")
	void invocationsAreDistinct() {
		var results = TestKit.run(TemplatesTestCase.class, SEED_42);

		results.testEvents().assertStatistics(stats -> stats.succeeded(6));
		assertThat(TestKit.reportValues(results, Seeds.SEED_REPORT_KEY)).hasSize(6).doesNotHaveDuplicates();
	}

	@Test
	@DisplayName("given a long parameter without @Seed when run then the extension does not claim it")
	void unsupportedParametersAreIgnored() {
		var results = TestKit.run(UnsupportedTestCase.class, SEED_42);

		results.testEvents().assertStatistics(stats -> stats.failed(1));
		assertThat(results.testEvents().failed().stream()
				.map(e -> e.getRequiredPayload(org.junit.platform.engine.TestExecutionResult.class).getThrowable().orElseThrow()))
				.singleElement()
				.satisfies(t -> assertThat(t).hasMessageContaining("No ParameterResolver registered"));
	}

	private static List<String> values(EngineExecutionResults results) {
		results.testEvents().assertStatistics(stats -> stats.failed(0));
		return TestKit.reportValues(results, "value");
	}

	@RandomSeeded
	@Tag("example")
	static class AllTypesTestCase {

		@Test
		void test(Random random, RandomGenerator generator, @Seed long seed, @Seed Long boxedSeed, TestReporter reporter) {
			reporter.publishEntry("value", "random:" + random.nextLong());
			reporter.publishEntry("value", "generator:" + generator.nextLong());
			reporter.publishEntry("value", "seed:" + seed);
			reporter.publishEntry("value", "boxed:" + boxedSeed);
		}
	}

	@RandomSeeded
	@Tag("example")
	static class SeedOnlyTestCase {

		@Test
		void test(@Seed long seed, TestReporter reporter) {
			reporter.publishEntry("value", Long.toString(seed));
		}
	}

	@RandomSeeded
	@Tag("example")
	static class TwoRandomsTestCase {

		@Test
		void test(Random a, Random b, TestReporter reporter) {
			reporter.publishEntry("value", Long.toString(a.nextLong()));
			reporter.publishEntry("value", Long.toString(b.nextLong()));
		}
	}

	@RandomSeeded
	@Tag("example")
	static class LifecycleTestCase {

		@BeforeEach
		void setUp(Random random, TestReporter reporter) {
			reporter.publishEntry("value", Long.toString(random.nextLong()));
		}

		@Test
		void test(Random random, TestReporter reporter) {
			reporter.publishEntry("value", Long.toString(random.nextLong()));
		}
	}

	@RandomSeeded
	@Tag("example")
	static class TemplatesTestCase {

		@ParameterizedTest
		@ValueSource(strings = { "a", "b", "c" })
		void parameterized(String argument, Random random) {
			assertThat(argument).isIn("a", "b", "c");
			random.nextInt();
		}

		@RepeatedTest(3)
		void repeated(Random random) {
			random.nextInt();
		}
	}

	@RandomSeeded
	@Tag("example")
	static class UnsupportedTestCase {

		@Test
		void test(long notASeed) {
		}
	}
}
