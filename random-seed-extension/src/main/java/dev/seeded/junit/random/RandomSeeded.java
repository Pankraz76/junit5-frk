package dev.seeded.junit.random;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Registers the {@link RandomSeedExtension} for the annotated class, interface or method.
 *
 * <p>Seeded tests can declare parameters of type {@link java.util.Random},
 * {@link java.util.random.RandomGenerator}, or {@code long}/{@link Long} annotated with
 * {@link Seed}. Every injected value is derived from a single root seed per test run, so one
 * value reproduces all of them.
 *
 * @see RandomSeedExtension
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ExtendWith(RandomSeedExtension.class)
public @interface RandomSeeded {
}
