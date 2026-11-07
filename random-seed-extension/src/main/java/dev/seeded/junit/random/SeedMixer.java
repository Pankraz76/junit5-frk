package dev.seeded.junit.random;

import java.nio.charset.StandardCharsets;

/**
 * Derives independent, reproducible seeds from a root seed and a stable identifier.
 *
 * <p>The identifier and salt are hashed with 64-bit FNV-1a (a {@code 0} byte separates them
 * so that {@code ("ab", "c")} and {@code ("a", "bc")} differ). The hash is XOR-ed with the
 * root seed and passed through the SplitMix64 finalizer. The finalizer is a bijection with
 * full avalanche, so root seeds with weak low bits (such as {@link System#nanoTime()} on
 * platforms with coarse timers) still yield well-distributed derived seeds.
 */
final class SeedMixer {

	private static final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;

	private static final long FNV_PRIME = 0x100000001b3L;

	private static final byte SEPARATOR = 0;

	static long derive(long rootSeed, String uniqueId, String salt) {
		long hash = FNV_OFFSET_BASIS;
		hash = fnv1a(hash, uniqueId);
		hash = fnv1a(hash, SEPARATOR);
		hash = fnv1a(hash, salt);
		return mix64(rootSeed ^ mix64(hash));
	}

	private static long fnv1a(long hash, String value) {
		for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
			hash = fnv1a(hash, b);
		}
		return hash;
	}

	private static long fnv1a(long hash, byte b) {
		return (hash ^ (b & 0xff)) * FNV_PRIME;
	}

	/**
	 * SplitMix64 finalizer (Steele, Lea, Flood: "Fast Splittable Pseudorandom Number Generators").
	 */
	private static long mix64(long z) {
		z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
		z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
		return z ^ (z >>> 31);
	}

	private SeedMixer() {
	}
}
