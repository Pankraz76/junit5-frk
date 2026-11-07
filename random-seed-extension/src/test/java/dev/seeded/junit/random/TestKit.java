package dev.seeded.junit.random;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.util.List;
import java.util.Map;

import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

/**
 * Runs example test classes on the Jupiter engine.
 */
final class TestKit {

	static EngineExecutionResults run(Class<?> testClass, Map<String, String> configurationParameters) {
		return EngineTestKit.engine("junit-jupiter")
				.configurationParameters(configurationParameters)
				.selectors(selectClass(testClass))
				.execute();
	}

	static EngineExecutionResults run(Class<?> testClass) {
		return run(testClass, Map.of());
	}

	static List<String> reportValues(EngineExecutionResults results, String key) {
		return results.allEvents().reportingEntryPublished().stream()
				.map(event -> event.getRequiredPayload(ReportEntry.class).getKeyValuePairs())
				.filter(entries -> entries.containsKey(key))
				.map(entries -> entries.get(key))
				.toList();
	}

	static List<String> reportValues(EngineExecutionResults results, String key, String displayName) {
		return results.allEvents().reportingEntryPublished().stream()
				.filter(event -> event.getTestDescriptor().getDisplayName().equals(displayName))
				.map(event -> event.getRequiredPayload(ReportEntry.class).getKeyValuePairs())
				.filter(entries -> entries.containsKey(key))
				.map(entries -> entries.get(key))
				.toList();
	}

	private TestKit() {
	}
}
