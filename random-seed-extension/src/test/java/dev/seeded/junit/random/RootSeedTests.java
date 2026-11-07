package dev.seeded.junit.random;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.platform.testkit.engine.EngineExecutionResults;

@DisplayName("Root seed")
class RootSeedTests {

	private static final String SEED = Seeds.SEED_REPORT_KEY;

	private static final String ORDER_SEED = "junit.jupiter.execution.order.random.seed";

	@RegisterExtension
	final LogCapture log = new LogCapture();

	@Test
	@DisplayName("given an explicit and an orderer seed when running then the explicit seed wins")
	void explicitSeedWins() {
		var results = TestKit.run(TwoSeededTestsTestCase.class, Map.of(SEED, "42", ORDER_SEED, "7"));

		results.testEvents().assertStatistics(stats -> stats.succeeded(2));
		assertThat(TestKit.reportValues(results, Seeds.ROOT_REPORT_KEY)).containsExactly("42", "42");
	}

	@Test
	@DisplayName("given only the Jupiter orderer seed when running then it is reused as root seed")
	void reusesOrdererSeed() {
		var results = TestKit.run(TwoSeededTestsTestCase.class, Map.of(ORDER_SEED, "7"));

		results.testEvents().assertStatistics(stats -> stats.succeeded(2));
		assertThat(TestKit.reportValues(results, Seeds.ROOT_REPORT_KEY)).containsExactly("7", "7");
	}

	@Test
	@DisplayName("given no configuration when running several tests then one generated seed is used and logged once")
	void generatedSeedIsSharedAndLoggedOnce() {
		EngineExecutionResults results = TestKit.run(TwoSeededTestsTestCase.class);

		results.testEvents().assertStatistics(stats -> stats.succeeded(2));
		var roots = TestKit.reportValues(results, Seeds.ROOT_REPORT_KEY);
		assertThat(roots).hasSize(2).containsOnly(roots.get(0));
		assertThat(log.messages(Level.CONFIG)) //
				.singleElement(org.assertj.core.api.InstanceOfAssertFactories.STRING) //
				.contains("[" + roots.get(0) + "]") //
				.contains("-Drandomseed.seed=" + roots.get(0));
	}

	@Test
	@DisplayName("given an invalid seed when running then a warning is logged and a generated seed is used")
	void invalidSeedFallsBack() {
		var results = TestKit.run(TwoSeededTestsTestCase.class, Map.of(SEED, "not-a-number"));

		results.testEvents().assertStatistics(stats -> stats.succeeded(2));
		assertThat(log.messages(Level.WARNING)) //
				.singleElement(org.assertj.core.api.InstanceOfAssertFactories.STRING) //
				.contains(SEED) //
				.contains("not-a-number");
		var roots = TestKit.reportValues(results, Seeds.ROOT_REPORT_KEY);
		assertThat(roots).hasSize(2).containsOnly(roots.get(0));
	}

	@RandomSeeded
	@Tag("example")
	static class TwoSeededTestsTestCase {

		@Test
		void first(Random random) {
			random.nextInt();
		}

		@Test
		void second(Random random) {
			random.nextInt();
		}
	}
}
