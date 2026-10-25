package golf.user;

import golf.GolfTrackerApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = GolfTrackerApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:local-registration;DB_CLOSE_DELAY=-1",
        "spring.mail.host=localhost",
        "golf.app.base-url=http://localhost:8080",
        "golf.auth.local-registration.enabled=true"
})
@AutoConfigureMockMvc
class LocalRegistrationIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    void localAccountCanBeRegisteredAndUsedToSignIn() throws Exception {
        mvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Local testing only")))
                .andExpect(content().string(containsString("name=\"email\"")));

        mvc.perform(post("/register").with(csrf())
                        .param("email", "link-test@example.com")
                        .param("password", "GolfClub99")
                        .param("confirmPassword", "GolfClub99"))
                .andExpect(redirectedUrl("/login?registered=true"));

        mvc.perform(post("/login").with(csrf())
                        .param("username", "link-test@example.com")
                        .param("password", "GolfClub99"))
                .andExpect(redirectedUrl("/challenge"));
    }
}
