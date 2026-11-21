/*
 * Copyright 2015-2026 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params;

import static org.junit.jupiter.engine.Constants.DEFAULT_TEST_CLASS_ORDER_PROPERTY_NAME;
import static org.junit.platform.commons.test.GarbageCollectionTestingUtils.assertEventuallyGarbageCollected;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.engine.AbstractJupiterTestEngineTests;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Regression tests ensuring that test instances of {@link ParameterizedTest}
 * methods are not retained by the engine once their test class has completed.
 *
 * @since 5.14.5
 * @see <a href="https://github.com/junit-team/junit-framework/issues/6306">#6306</a>
 */
class ParameterizedTestInstanceRetentionTests extends AbstractJupiterTestEngineTests {

	private static final AtomicReference<WeakReference<Object>> parameterizedTestInstance = new AtomicReference<>();

	@AfterEach
	void resetState() {
		parameterizedTestInstance.set(null);
	}

	@Test
	void parameterizedTestInstanceIsNotRetainedWhileSubsequentTestClassIsExecuted() {
		// given: a test class with a @ParameterizedTest followed by another test class
		// when: both are executed in a single engine run
		var results = executeTests(request -> request //
				.selectors(selectClass(ParameterizedTestCase.class), selectClass(SubsequentTestCase.class)) //
				.configurationParameter(DEFAULT_TEST_CLASS_ORDER_PROPERTY_NAME,
					ClassOrderer.OrderAnnotation.class.getName()));

		// then: the subsequent test class could observe that the instance was garbage collected
		results.testEvents().failed().assertThatEvents().isEmpty();
		results.testEvents().assertStatistics(stats -> stats.started(2).succeeded(2));
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@Order(1)
	static class ParameterizedTestCase {

		@ParameterizedTest
		@ValueSource(ints = 1)
		void test(int number) {
			parameterizedTestInstance.set(new WeakReference<>(this));
		}
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@Order(2)
	static class SubsequentTestCase {

		@Test
		void test() throws Exception {
			assertEventuallyGarbageCollected(parameterizedTestInstance.get(),
				"instance of previously executed parameterized test");
		}
	}

}
