package golf.email;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class ProdMailSecurityConfigTest {

    @Test
    void productionProfileEnforcesAuthenticatedStartTls() {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application-prod.yml"));
        Properties properties = factory.getObject();

        assertThat(properties).isNotNull();
        assertThat(properties.getProperty("spring.mail.properties.mail.smtp.auth")).isEqualTo("true");
        assertThat(properties.getProperty("spring.mail.properties.mail.smtp.starttls.enable")).isEqualTo("true");
        assertThat(properties.getProperty("spring.mail.properties.mail.smtp.starttls.required")).isEqualTo("true");
    }
}
