package dev.seeded.junit.random;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * {@code @RandomNumberSource} is an {@link ArgumentsSource} that provides reproducible random
 * numbers to the first parameter of a {@link org.junit.jupiter.params.ParameterizedTest}.
 *
 * <p>The element type is taken from the first parameter: {@code int}, {@code long},
 * {@code double} or their wrappers. Values are drawn from {@code [min, max)}. Unset bounds
 * default to the full range of {@code int} or {@code long} (the type's maximum value is then
 * excluded), and to {@code [0.0, 1.0)} for {@code double}.
 *
 * <p>The values are derived from the root seed of the run and the unique id of the
 * parameterized test, so they are stable across runs with the same {@code randomseed.seed}.
 * Annotating a method with {@code @RandomNumberSource} also registers the
 * {@link RandomSeedExtension} for it.
 *
 * <pre>{@code
 * @ParameterizedTest
 * @RandomNumberSource(count = 100, min = 0, max = 1_000)
 * void roundTrips(int value) { ... }
 * }</pre>
 */
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ArgumentsSource(RandomNumberArgumentsProvider.class)
@ExtendWith(RandomSeedExtension.class)
public @interface RandomNumberSource {

	/**
	 * Number of values to generate; must be greater than {@code 0}.
	 *
	 * @return the number of values
	 */
	int count() default 10;

	/**
	 * Inclusive lower bound.
	 *
	 * @return the lower bound
	 */
	long min() default Long.MIN_VALUE;

	/**
	 * Exclusive upper bound.
	 *
	 * @return the upper bound
	 */
	long max() default Long.MAX_VALUE;
}
