package golf;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestContainersConfiguration {

	@Bean
	GenericContainer<?> mailpit() {
		return new GenericContainer<>(DockerImageName.parse("axllent/mailpit:latest"))
				.withExposedPorts(1025, 8025);
	}

	@Bean
	DynamicPropertyRegistrar mailpitProperties(GenericContainer<?> mailpit) {
		return registry -> {
			registry.add("spring.mail.host", mailpit::getHost);
			registry.add("spring.mail.port", () -> mailpit.getMappedPort(1025));
		};
	}

}
