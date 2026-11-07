package dev.seeded.junit.random;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

/**
 * Derives per-use seeds for an extension context and records them, so that a failing test can
 * report the seeds it (or one of its enclosing containers) used.
 */
final class Seeds {

	static final String ROOT_REPORT_KEY = "randomseed.root";

	static final String SEED_REPORT_KEY = "randomseed.seed";

	private static final Namespace NAMESPACE = Namespace.create(Seeds.class);

	static long derive(ExtensionContext context, String salt) {
		long root = RootSeed.get(context);
		long seed = SeedMixer.derive(root, context.getUniqueId(), salt);
		context.publishReportEntry(Map.of(ROOT_REPORT_KEY, Long.toString(root), SEED_REPORT_KEY, Long.toString(seed)));
		// Keyed by unique id: store lookups fall back to ancestors, which must not be mutated here.
		context.getStore(NAMESPACE).computeIfAbsent(context.getUniqueId(), __ -> new Used(), Used.class).add(seed);
		return seed;
	}

	/**
	 * Seeds used by the given context and its ancestors, innermost first.
	 */
	static List<Long> usedBy(ExtensionContext context) {
		List<Long> seeds = new ArrayList<>();
		for (Optional<ExtensionContext> current = Optional.of(context); current.isPresent(); current = current.get().getParent()) {
			ExtensionContext ctx = current.get();
			Used used = ctx.getStore(NAMESPACE).get(ctx.getUniqueId(), Used.class);
			if (used != null) {
				seeds.addAll(used.seeds);
			}
		}
		return seeds;
	}

	private static final class Used {

		private final List<Long> seeds = new CopyOnWriteArrayList<>();

		void add(long seed) {
			seeds.add(seed);
		}
	}

	private Seeds() {
	}
}
