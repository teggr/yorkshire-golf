package golf;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationPropertiesTest {

    @Test
    void productionFacingPropertiesAreDrivenByEnvironmentVariables() throws IOException {
        Properties properties = new Properties();

        try (var input = Files.newInputStream(Path.of("src/main/resources/application.properties"))) {
            properties.load(input);
        }

        assertEquals("${MAIL_SMTP_AUTH:false}", properties.getProperty("spring.mail.properties.mail.smtp.auth"));
        assertEquals("${MAIL_SMTP_STARTTLS_ENABLE:false}", properties.getProperty("spring.mail.properties.mail.smtp.starttls.enable"));
        assertEquals("${MAIL_SMTP_STARTTLS_REQUIRED:false}", properties.getProperty("spring.mail.properties.mail.smtp.starttls.required"));
        assertEquals("${MAIL_FROM:noreply@email.yorkshiregolf.life}", properties.getProperty("golf.mail.from"));
        assertEquals("${APP_BASE_URL:http://localhost:8080}", properties.getProperty("golf.app.base-url"));
    }
}