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

    @Test
    void challengeLandingIsPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/challenge"))
                .andExpect(status().isOk());
    }

    @Test
    void mapPageIsPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/map"))
                .andExpect(status().isOk());
    }

    @Test
    void searchPageIsPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/search").param("q", "leeds"))
                .andExpect(status().isOk());
    }

    @Test
    void challengeTrackerPathIsPublicAndReturnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get("/challenge/unknown-tracker"))
                .andExpect(status().isNotFound());
    }

        @Test
        void roundsRoutesAreNotFound() throws Exception {
        mockMvc.perform(get("/rounds"))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/rounds/legacy-id"))
            .andExpect(status().isNotFound());
        }

        @Test
        void myRoundsRoutesAreNotFound() throws Exception {
            mockMvc.perform(get("/my-rounds"))
                    .andExpect(status().isNotFound());

            mockMvc.perform(get("/my-rounds/add"))
                    .andExpect(status().isNotFound());
        }

    @Test
    void legacyTrackerPathNowRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/tracker/unknown-tracker"))
                .andExpect(status().is3xxRedirection());
    }
}