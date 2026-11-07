package dev.seeded.junit.random;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code long} or {@link Long} parameter that should receive the derived seed of the
 * current test, for code that creates its own random number generator.
 *
 * @see RandomSeeded
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Seed {
}
