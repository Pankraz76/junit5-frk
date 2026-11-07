package dev.seeded.junit.random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;

@DisplayName("Failure reporting")
class FailureReportingTests {

	private static final Map<String, String> SEED_42 = Map.of(RootSeed.SEED_PROPERTY_NAME, "42");

	@RegisterExtension
	final LogCapture log = new LogCapture();

	@Test
	@DisplayName("given a failing test that used randomness then a warning names the test, its seed and how to reproduce")
	void failingSeededTestIsReported() {
		var results = TestKit.run(FailingSeededTestCase.class, SEED_42);

		results.testEvents().assertStatistics(stats -> stats.failed(1).succeeded(1));
		var seedOfFailingTest = TestKit.reportValues(results, Seeds.SEED_REPORT_KEY, "failingWithRandom(Random)");
		assertThat(seedOfFailingTest).hasSize(1);
		assertThat(log.messages(Level.WARNING)).singleElement(STRING)
				.contains("failingWithRandom(Random)")
				.contains("[" + seedOfFailingTest.get(0) + "]")
				.contains("root seed [42]")
				.contains("-Drandomseed.seed=42");
	}

	@Test
	@DisplayName("given a failing invocation of a @RandomNumberSource test then a warning is logged")
	void failingSourceInvocationIsReported() {
		var results = TestKit.run(FailingSourceTestCase.class, SEED_42);

		results.testEvents().assertStatistics(stats -> stats.failed(2));
		var derived = TestKit.reportValues(results, Seeds.SEED_REPORT_KEY);
		assertThat(log.messages(Level.WARNING)).hasSize(2)
				.allSatisfy(message -> assertThat(message).contains("[" + derived.get(0) + "]").contains("-Drandomseed.seed=42"));
	}

	@Test
	@DisplayName("given a failing test that used no randomness then nothing is reported")
	void failingUnseededTestIsNotReported() {
		var results = TestKit.run(FailingUnseededTestCase.class, SEED_42);

		results.testEvents().assertStatistics(stats -> stats.failed(1));
		assertThat(log.messages(Level.WARNING)).isEmpty();
	}

	@RandomSeeded
	@Tag("example")
	static class FailingSeededTestCase {

		@Test
		void failingWithRandom(Random random) {
			fail("boom " + random.nextInt());
		}

		@Test
		void passingWithRandom(Random random) {
			random.nextInt();
		}
	}

	@Tag("example")
	static class FailingSourceTestCase {

		@ParameterizedTest
		@RandomNumberSource(count = 2)
		void test(int value) {
			fail("boom " + value);
		}
	}

	@RandomSeeded
	@Tag("example")
	static class FailingUnseededTestCase {

		@Test
		void failingWithoutRandom() {
			fail("boom");
		}
	}
}
