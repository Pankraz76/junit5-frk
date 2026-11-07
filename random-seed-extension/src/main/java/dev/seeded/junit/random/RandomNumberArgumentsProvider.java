package dev.seeded.junit.random;

import java.util.SplittableRandom;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.AnnotationBasedArgumentsProvider;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.support.ParameterDeclaration;
import org.junit.jupiter.params.support.ParameterDeclarations;

/**
 * Provides the arguments for {@link RandomNumberSource}.
 */
class RandomNumberArgumentsProvider extends AnnotationBasedArgumentsProvider<RandomNumberSource> {

	private static final String SALT = RandomNumberSource.class.getName();

	@Override
	protected Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context,
			RandomNumberSource annotation) {
		String element = parameters.getSourceElementDescription();
		if (annotation.count() < 1) {
			throw configurationError(element, "count must be greater than 0, but was [%d]".formatted(annotation.count()));
		}
		ParameterDeclaration parameter = parameters.getFirst().orElseThrow(
			() -> configurationError(element, "the method must declare at least one parameter"));
		Supplier<Object> values = valueSupplier(element, parameter.getParameterType(), annotation,
			new SplittableRandom(Seeds.derive(context, SALT)));
		return Stream.generate(values).limit(annotation.count()).map(Arguments::of);
	}

	private static Supplier<Object> valueSupplier(String element, Class<?> type, RandomNumberSource annotation,
			SplittableRandom random) {
		boolean minSet = annotation.min() != Long.MIN_VALUE;
		boolean maxSet = annotation.max() != Long.MAX_VALUE;
		if (type == int.class || type == Integer.class) {
			long min = minSet ? annotation.min() : Integer.MIN_VALUE;
			long max = maxSet ? annotation.max() : Integer.MAX_VALUE;
			if (min < Integer.MIN_VALUE || max > Integer.MAX_VALUE) {
				throw configurationError(element,
					"range [%d, %d) does not fit into int parameter; use a long parameter instead".formatted(min, max));
			}
			checkRange(element, min, max);
			return () -> random.nextInt((int) min, (int) max);
		}
		if (type == long.class || type == Long.class) {
			checkRange(element, annotation.min(), annotation.max());
			return () -> random.nextLong(annotation.min(), annotation.max());
		}
		if (type == double.class || type == Double.class) {
			double min = minSet ? annotation.min() : 0.0;
			double max = maxSet ? annotation.max() : 1.0;
			checkRange(element, min, max);
			return () -> random.nextDouble(min, max);
		}
		throw configurationError(element, "unsupported parameter type [%s]; supported are int, long and double (and their wrappers)"
				.formatted(type.getName()));
	}

	private static void checkRange(String element, long min, long max) {
		if (min >= max) {
			throw invalidRange(element, min, max);
		}
	}

	private static void checkRange(String element, double min, double max) {
		if (min >= max) {
			throw invalidRange(element, min, max);
		}
	}

	private static ExtensionConfigurationException invalidRange(String element, Object min, Object max) {
		return configurationError(element, "min [%s] must be less than max [%s]".formatted(min, max));
	}

	private static ExtensionConfigurationException configurationError(String element, String detail) {
		return new ExtensionConfigurationException("@RandomNumberSource on %s: %s".formatted(element, detail));
	}
}
