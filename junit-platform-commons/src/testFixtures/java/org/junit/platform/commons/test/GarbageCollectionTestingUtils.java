/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.commons.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.ref.WeakReference;

public class GarbageCollectionTestingUtils {

	private static final int MAX_ATTEMPTS = 50;
	private static final long SLEEP_MILLIS_BETWEEN_ATTEMPTS = 10;

	/**
	 * Assert that the object referenced by the supplied {@link WeakReference}
	 * is garbage collected after repeatedly requesting a garbage collection,
	 * i.e. that it is no longer strongly reachable.
	 */
	public static void assertEventuallyGarbageCollected(WeakReference<?> reference, String description)
			throws InterruptedException {

		assertThat(reference).as("reference").isNotNull();
		for (int attempt = 0; attempt < MAX_ATTEMPTS && reference.get() != null; attempt++) {
			System.gc();
			Thread.sleep(SLEEP_MILLIS_BETWEEN_ATTEMPTS);
		}
		assertThat(reference.get()).as("%s should have been garbage collected", description).isNull();
	}

	private GarbageCollectionTestingUtils() {
	}
}
