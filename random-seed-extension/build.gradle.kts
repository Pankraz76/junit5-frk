plugins {
	`java-library`
}

group = "dev.seeded"
version = "0.1.0-SNAPSHOT"

repositories {
	mavenCentral()
}

val junitVersion = "6.1.3"

dependencies {
	api(platform("org.junit:junit-bom:$junitVersion"))
	api("org.junit.jupiter:junit-jupiter-api")
	api("org.junit.jupiter:junit-jupiter-params")
	compileOnly("org.jspecify:jspecify:1.0.0")

	testImplementation("org.junit.jupiter:junit-jupiter")
	testImplementation("org.junit.platform:junit-platform-testkit")
	testImplementation("org.assertj:assertj-core:3.27.6")
	testCompileOnly("org.jspecify:jspecify:1.0.0")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
	withSourcesJar()
	withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 17
	options.encoding = "UTF-8"
	options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

tasks.javadoc {
	(options as StandardJavadocDocletOptions).addBooleanOption("Werror", true)
}

tasks.test {
	useJUnitPlatform {
		// Example test classes executed via EngineTestKit only
		excludeTags("example")
	}
	exclude("**/*TestCase.class")
}
