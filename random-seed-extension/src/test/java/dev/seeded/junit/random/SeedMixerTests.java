package dev.seeded.junit.random;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Seed derivation")
class SeedMixerTests {

	private static final String ID = "[engine:junit-jupiter]/[class:a.FooTests]/[method:bar()]";

	@Test
	@DisplayName("given the same inputs when deriving twice then the results are equal")
	void deterministic() {
		assertThat(SeedMixer.derive(42L, ID, "p0")).isEqualTo(SeedMixer.derive(42L, ID, "p0"));
	}

	@Test
	@DisplayName("given one root seed when deriving for different ids or salts then all seeds differ")
	void distinct() {
		Set<Long> seeds = new HashSet<>();
		for (int i = 0; i < 1_000; i++) {
			seeds.add(SeedMixer.derive(42L, ID + "/[test-template-invocation:#" + i + "]", "p0"));
			seeds.add(SeedMixer.derive(42L, ID + "/[test-template-invocation:#" + i + "]", "p1"));
		}
		assertThat(seeds).hasSize(2_000);
	}

	@Test
	@DisplayName("given id/salt boundary shifts when deriving then seeds differ")
	void saltBoundaryIsUnambiguous() {
		assertThat(SeedMixer.derive(42L, "ab", "c")).isNotEqualTo(SeedMixer.derive(42L, "a", "bc"));
	}

	@Test
	@DisplayName("given root seeds differing in one low bit when deriving then about half of all bits flip")
	void avalanche() {
		long totalFlipped = 0;
		int samples = 256;
		for (int i = 0; i < samples; i++) {
			long root = 1_000_000_000L * i; // nanoTime-like values with zero low decimal digits
			totalFlipped += Long.bitCount(SeedMixer.derive(root, ID, "p0") ^ SeedMixer.derive(root ^ 1L, ID, "p0"));
		}
		double averageFlipped = (double) totalFlipped / samples;
		assertThat(averageFlipped).isBetween(28.0, 36.0);
	}
}
