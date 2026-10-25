package golf.user;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterPageTest {
    @Test
    void localRegistrationFormOnlyAppearsWhenExplicitlyEnabledForLoopback() throws Exception {
        var providers = new golf.security.FederatedProviders(new MockEnvironment());
        var page = new RegisterPage(providers, new LocalRegistration(true, "http://localhost:8080"));
        var request = new MockHttpServletRequest();
        request.setRequestURI("/register");
        var response = new MockHttpServletResponse();

        page.render(Map.of("localRegistrationEnabled", true), request, response);

        assertThat(response.getContentAsString())
                .contains("Local testing only", "name=\"email\"", "name=\"password\"", "name=\"confirmPassword\"")
                .doesNotContain("No sign-in providers are available");
    }
}
