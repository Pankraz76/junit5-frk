package dev.seeded.junit.random;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Test-only extension that captures JUL records of this library while a test runs.
 */
final class LogCapture implements BeforeEachCallback, AfterEachCallback {

	private static final Logger LOGGER = Logger.getLogger("dev.seeded.junit.random");

	private final List<LogRecord> records = new CopyOnWriteArrayList<>();

	private final Handler handler = new Handler() {

		@Override
		public void publish(LogRecord logRecord) {
			records.add(logRecord);
		}

		@Override
		public void flush() {
		}

		@Override
		public void close() {
		}
	};

	private Level previousLevel = Level.INFO;

	@Override
	public void beforeEach(ExtensionContext context) {
		records.clear();
		previousLevel = LOGGER.getLevel();
		handler.setLevel(Level.ALL);
		LOGGER.setLevel(Level.ALL);
		LOGGER.addHandler(handler);
	}

	@Override
	public void afterEach(ExtensionContext context) {
		LOGGER.removeHandler(handler);
		LOGGER.setLevel(previousLevel);
	}

	List<String> messages(Level level) {
		return records.stream()
				.filter(r -> r.getLevel().equals(level))
				.map(r -> r.getMessage())
				.toList();
	}
}
