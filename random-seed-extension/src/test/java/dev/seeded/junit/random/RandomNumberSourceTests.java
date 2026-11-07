package dev.seeded.junit.random;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineExecutionResults;

@DisplayName("@RandomNumberSource")
class RandomNumberSourceTests {

	private static final Map<String, String> SEED_42 = Map.of(RootSeed.SEED_PROPERTY_NAME, "42");

	@Nested
	@DisplayName("given a valid declaration")
	class Valid {

		@Test
		@DisplayName("when the parameter is int, Integer, long, Long, double or Double then count values of that type are generated")
		void generatesDeclaredType() {
			var results = TestKit.run(TypesTestCase.class, SEED_42);

			results.testEvents().assertStatistics(stats -> stats.succeeded(6 * 5).failed(0));
			assertThat(TestKit.reportValues(results, "value")).hasSize(30).doesNotHaveDuplicates();
		}

		@Test
		@DisplayName("when min and max are set then every value v satisfies min <= v < max")
		void respectsRange() {
			var results = TestKit.run(RangeTestCase.class, SEED_42);

			results.testEvents().assertStatistics(stats -> stats.succeeded(3 * 50).failed(0));
		}

		@Test
		@DisplayName("when long bounds are adjacent near Long.MAX_VALUE then they are not confused by floating-point rounding")
		void longRangeKeepsFullPrecision() {
			TestKit.run(LongPrecisionTestCase.class, SEED_42)
					.testEvents().assertStatistics(stats -> stats.succeeded(3).failed(0));
		}

		@Test
		@DisplayName("when count is omitted then 10 values are generated")
		void defaultCount() {
			TestKit.run(DefaultCountTestCase.class, SEED_42)
					.testEvents().assertStatistics(stats -> stats.succeeded(10));
		}

		@Test
		@DisplayName("when run twice with the same root seed then the same values are generated")
		void reproducible() {
			var first = TestKit.reportValues(TestKit.run(TypesTestCase.class, SEED_42), "value");
			var second = TestKit.reportValues(TestKit.run(TypesTestCase.class, SEED_42), "value");

			assertThat(first).hasSize(30).isEqualTo(second);
		}

		@Test
		@DisplayName("when other parameters follow then they are resolved by other resolvers")
		void coexistsWithInjection() {
			TestKit.run(MixedTestCase.class, SEED_42)
					.testEvents().assertStatistics(stats -> stats.succeeded(3));
		}
	}

	@Nested
	@DisplayName("given an invalid declaration then the test template fails with a descriptive message")
	class Invalid {

		@Test
		@DisplayName("when count < 1")
		void count() {
			assertConfigurationError(InvalidCountTestCase.class, "count must be greater than 0");
		}

		@Test
		@DisplayName("when min >= max")
		void range() {
			assertConfigurationError(InvalidRangeTestCase.class, "min [5] must be less than max [5]");
		}

		@Test
		@DisplayName("when the parameter type is not supported")
		void type() {
			assertConfigurationError(UnsupportedTypeTestCase.class, "java.lang.String");
		}

		@Test
		@DisplayName("when the method declares no parameters")
		void noParameter() {
			assertConfigurationError(NoParameterTestCase.class, "at least one parameter");
		}

		@Test
		@DisplayName("when an explicit range does not fit an int parameter")
		void intOverflow() {
			assertConfigurationError(IntOverflowTestCase.class, "does not fit into int");
		}

		private void assertConfigurationError(Class<?> testClass, String messagePart) {
			EngineExecutionResults results = TestKit.run(testClass, SEED_42);

			assertThat(results.containerEvents().failed().stream()
					.map(e -> e.getRequiredPayload(TestExecutionResult.class).getThrowable().orElseThrow()))
					.singleElement()
					.satisfies(t -> assertThat(t).isInstanceOf(ExtensionConfigurationException.class)
							.hasMessageContaining(messagePart));
		}
	}

	@Tag("example")
	static class TypesTestCase {

		@ParameterizedTest
		@RandomNumberSource(count = 5)
		void primitiveInt(int value, TestReporter reporter) {
			reporter.publishEntry("value", "int:" + value);
		}

		@ParameterizedTest
		@RandomNumberSource(count = 5)
		void boxedInt(Integer value, TestReporter reporter) {
			reporter.publishEntry("value", "Integer:" + value);
		}

		@ParameterizedTest
		@RandomNumberSource(count = 5)
		void primitiveLong(long value, TestReporter reporter) {
			reporter.publishEntry("value", "long:" + value);
		}

		@ParameterizedTest
		@RandomNumberSource(count = 5)
		void boxedLong(Long value, TestReporter reporter) {
			reporter.publishEntry("value", "Long:" + value);
		}

		@ParameterizedTest
		@RandomNumberSource(count = 5)
		void primitiveDouble(double value, TestReporter reporter) {
			assertThat(value).isGreaterThanOrEqualTo(0.0).isLessThan(1.0);
			reporter.publishEntry("value", "double:" + value);
		}

		@ParameterizedTest
		@RandomNumberSource(count = 5)
		void boxedDouble(Double value, TestReporter reporter) {
			reporter.publishEntry("value", "Double:" + value);
		}
	}

	@Tag("example")
	static class RangeTestCase {

		@ParameterizedTest
		@RandomNumberSource(count = 50, min = 10, max = 20)
		void intRange(int value) {
			assertThat(value).isBetween(10, 19);
		}

		@ParameterizedTest
		@RandomNumberSource(count = 50, min = -3, max = -1)
		void longRange(long value) {
			assertThat(value).isBetween(-3L, -2L);
		}

		@ParameterizedTest
		@RandomNumberSource(count = 50, min = 1, max = 2)
		void doubleRange(double value) {
			assertThat(value).isGreaterThanOrEqualTo(1.0).isLessThan(2.0);
		}
	}

	@Tag("example")
	static class LongPrecisionTestCase {

		@ParameterizedTest
		@RandomNumberSource(count = 3, min = Long.MAX_VALUE - 2, max = Long.MAX_VALUE - 1)
		void test(long value) {
			assertThat(value).isEqualTo(Long.MAX_VALUE - 2);
		}
	}

	@Tag("example")
	static class DefaultCountTestCase {

		@ParameterizedTest
		@RandomNumberSource
		void test(long value) {
		}
	}

	@Tag("example")
	static class MixedTestCase {

		@ParameterizedTest
		@RandomNumberSource(count = 3)
		void test(int value, java.util.Random random, TestReporter reporter) {
			random.nextInt();
		}
	}

	@Tag("example")
	static class InvalidCountTestCase {

		@ParameterizedTest
		@RandomNumberSource(count = 0)
		void test(int value) {
		}
	}

	@Tag("example")
	static class InvalidRangeTestCase {

		@ParameterizedTest
		@RandomNumberSource(min = 5, max = 5)
		void test(int value) {
		}
	}

	@Tag("example")
	static class UnsupportedTypeTestCase {

		@ParameterizedTest
		@RandomNumberSource
		void test(String value) {
		}
	}

	@Tag("example")
	static class NoParameterTestCase {

		@ParameterizedTest
		@RandomNumberSource
		void test() {
		}
	}

	@Tag("example")
	static class IntOverflowTestCase {

		@ParameterizedTest
		@RandomNumberSource(min = 0, max = 5_000_000_000L)
		void test(int value) {
		}
	}
}
