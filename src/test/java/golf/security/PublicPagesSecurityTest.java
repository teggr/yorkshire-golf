package golf.security;

import golf.GolfTrackerApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {GolfTrackerApplication.class, PublicPagesSecurityTest.TestMailConfig.class})
@AutoConfigureMockMvc
class PublicPagesSecurityTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class TestMailConfig {
        @Bean
        JavaMailSender javaMailSender() {
            return new JavaMailSenderImpl();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void top100PageIsPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/top-100"))
                .andExpect(status().isOk());
    }

    @Test
    void next100PageIsPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/next-100"))
                .andExpect(status().isOk());
    }
}