package golf;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestContainersConfiguration {

	@Bean(initMethod = "start", destroyMethod = "stop")
	GenericContainer<?> mailpit() {
		return new GenericContainer<>(DockerImageName.parse("axllent/mailpit:latest"))
				.withExposedPorts(1025, 8025);
	}

	@Bean
	JavaMailSender javaMailSender(GenericContainer<?> mailpit) {
		JavaMailSenderImpl sender = new JavaMailSenderImpl();
		sender.setHost(mailpit.getHost());
		sender.setPort(mailpit.getMappedPort(1025));
		return sender;
	}

	@Bean
	DynamicPropertyRegistrar mailpitProperties(GenericContainer<?> mailpit) {
		return registry -> {
			registry.add("spring.mail.host", mailpit::getHost);
			registry.add("spring.mail.port", () -> mailpit.getMappedPort(1025));
		};
	}

}
