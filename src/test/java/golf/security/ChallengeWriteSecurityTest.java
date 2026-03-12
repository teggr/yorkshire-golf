package golf.security;

import golf.GolfTrackerApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {GolfTrackerApplication.class, ChallengeWriteSecurityTest.TestMailConfig.class})
@AutoConfigureMockMvc
class ChallengeWriteSecurityTest {

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
    void addRoundPostRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/challenge/unknown-tracker/add-round")
                        .param("courseName", "Alwoodley")
                        .param("date", "2026-03-12")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void deleteRoundPostRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/challenge/unknown-tracker/delete-round")
                        .param("roundId", "1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void updateRoundDatePostRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/challenge/unknown-tracker/update-round-date")
                        .param("roundId", "1")
                        .param("date", "2026-03-12")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void importRoundsPostRequiresAuthentication() throws Exception {
        MockMultipartFile csv = new MockMultipartFile(
                "csvFile",
                "rounds.csv",
                "text/csv",
                "courseName,date\nAlwoodley,2026-03-12\n".getBytes()
        );

        mockMvc.perform(multipart("/challenge/unknown-tracker/import-rounds")
                        .file(csv)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void authenticatedUserCanReachChallengeWriteRoutes() throws Exception {
        mockMvc.perform(post("/challenge/unknown-tracker/add-round")
                        .param("courseName", "Alwoodley")
                        .param("date", "2026-03-12")
                        .with(user("user@example.com").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/challenge/unknown-tracker/delete-round")
                        .param("roundId", "1")
                        .with(user("user@example.com").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/challenge/unknown-tracker/update-round-date")
                        .param("roundId", "1")
                        .param("date", "2026-03-12")
                        .with(user("user@example.com").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isNotFound());

        MockMultipartFile csv = new MockMultipartFile(
                "csvFile",
                "rounds.csv",
                "text/csv",
                "courseName,date\nAlwoodley,2026-03-12\n".getBytes()
        );

        mockMvc.perform(multipart("/challenge/unknown-tracker/import-rounds")
                        .file(csv)
                        .with(user("user@example.com").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }
}
