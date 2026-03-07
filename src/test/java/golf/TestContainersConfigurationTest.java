package golf;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class TestContainersConfigurationTest {

    @Test
    void providesJavaMailSenderBean() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(TestContainersConfiguration.class);
            context.refresh();

            JavaMailSender mailSender = context.getBean(JavaMailSender.class);
            assertNotNull(mailSender);
        }
    }
}
