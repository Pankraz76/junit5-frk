package dev.seeded.junit.random;

import java.util.List;
import java.util.Random;
import java.util.SplittableRandom;
import java.util.logging.Logger;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestWatcher;

/**
 * Injects reproducible randomness into tests.
 *
 * <p>Supported parameters (in constructors, lifecycle methods and test methods):
 * <ul>
 * <li>{@link Random}: a new {@code java.util.Random} seeded with the derived seed</li>
 * <li>{@link RandomGenerator}: a new {@link SplittableRandom} seeded with the derived seed</li>
 * <li>{@code long} / {@link Long} annotated with {@link Seed}: the derived seed itself</li>
 * </ul>
 *
 * <p>The derived seed depends on the root seed of the run (see {@code randomseed.seed}), the
 * unique id of the current test or container, the declaring executable, and the parameter
 * index. It is therefore stable across runs, and distinct for every test, every invocation of
 * a parameterized or repeated test, every lifecycle method, and every injected parameter.
 * Every derived seed is published as report entry {@code randomseed.seed}, together with the
 * root seed as {@code randomseed.root}.
 *
 * <p>When a test fails after it, one of its enclosing containers, or a
 * {@link RandomNumberSource} used a derived seed, a warning is logged with the seeds involved
 * and the command-line flag that reproduces the run.
 *
 * @see RandomSeeded
 */
public final class RandomSeedExtension implements ParameterResolver, TestWatcher {

	private static final Logger LOGGER = Logger.getLogger(RandomSeedExtension.class.getPackageName());

	/**
	 * Creates the extension; it is usually registered via {@link RandomSeeded} or
	 * {@link RandomNumberSource}.
	 */
	public RandomSeedExtension() {
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		Class<?> type = parameterContext.getParameter().getType();
		return type == Random.class || type == RandomGenerator.class
				|| ((type == long.class || type == Long.class) && parameterContext.isAnnotated(Seed.class));
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		long seed = Seeds.derive(extensionContext,
			parameterContext.getDeclaringExecutable() + "#" + parameterContext.getIndex());
		Class<?> type = parameterContext.getParameter().getType();
		if (type == Random.class) {
			return new Random(seed);
		}
		if (type == RandomGenerator.class) {
			return new SplittableRandom(seed);
		}
		return seed;
	}

	@Override
	public void testFailed(ExtensionContext context, Throwable cause) {
		List<Long> seeds = Seeds.usedBy(context);
		if (seeds.isEmpty()) {
			return;
		}
		long root = RootSeed.get(context);
		LOGGER.warning(() -> """
				Test [%s] failed after using random seed(s) %s derived from root seed [%d]. \
				To reproduce, pass -D%s=%3$d. Unique id: %s""".formatted(context.getDisplayName(), seeds, root,
			RootSeed.SEED_PROPERTY_NAME, context.getUniqueId()));
	}
}
