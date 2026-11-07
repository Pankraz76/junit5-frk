package dev.seeded.junit.random;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.StoreScope;

/**
 * Resolves the single root seed of a launcher session.
 *
 * <p>Precedence: {@value #SEED_PROPERTY_NAME}, then {@value #ORDERER_SEED_PROPERTY_NAME}, then a
 * value generated from {@link System#nanoTime()}. The result is cached in the launcher-session
 * store, so it is resolved (and logged) once per session, however many tests use it.
 */
final class RootSeed {

	static final String SEED_PROPERTY_NAME = "randomseed.seed";

	static final String ORDERER_SEED_PROPERTY_NAME = "junit.jupiter.execution.order.random.seed";

	private static final Logger LOGGER = Logger.getLogger(RootSeed.class.getPackageName());

	private static final Namespace NAMESPACE = Namespace.create(RootSeed.class);

	static long get(ExtensionContext context) {
		return context.getStore(StoreScope.LAUNCHER_SESSION, NAMESPACE)
				.computeIfAbsent(RootSeed.class, __ -> resolve(context), Long.class);
	}

	private static long resolve(ExtensionContext context) {
		long seed = configured(context, SEED_PROPERTY_NAME)
				.or(() -> configured(context, ORDERER_SEED_PROPERTY_NAME))
				.orElseGet(System::nanoTime);
		LOGGER.config(() -> """
				Using random root seed [%d]. To reproduce this run, pass -D%s=%1$d \
				(or set configuration parameter '%2$s=%1$d').""".formatted(seed, SEED_PROPERTY_NAME));
		return seed;
	}

	private static Optional<Long> configured(ExtensionContext context, String key) {
		return context.getConfigurationParameter(key).map(String::strip).flatMap(value -> {
			try {
				return Optional.of(Long.decode(value));
			}
			catch (NumberFormatException ex) {
				LOGGER.log(Level.WARNING, ex, () -> """
						Ignoring configuration parameter [%s] with value [%s]: not a valid long.""".formatted(key, value));
				return Optional.empty();
			}
		});
	}

	private RootSeed() {
	}
}
