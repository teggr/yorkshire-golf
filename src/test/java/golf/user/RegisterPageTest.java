package golf.user;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterPageTest {
    @Test
    void registrationGroupsOnlyEnabledSocialProvidersSeparatelyFromLocalEmailForm() throws Exception {
        var providers = new golf.security.FederatedProviders(new MockEnvironment()
                .withProperty("golf.app.base-url", "http://localhost:8080")
                .withProperty("golf.auth.x.enabled", "true")
                .withProperty("golf.auth.x.client-id", "test-client")
                .withProperty("golf.auth.x.client-secret", "test-secret")
                .withProperty("golf.auth.google.enabled", "true")
                .withProperty("golf.auth.google.client-id", "google-client")
                .withProperty("golf.auth.google.client-secret", "google-secret"));
        var page = new RegisterPage(providers, new LocalRegistration(true, "http://localhost:8080"));
        var request = new MockHttpServletRequest();
        request.setRequestURI("/register");
        var response = new MockHttpServletResponse();

        page.render(Map.of("localRegistrationEnabled", true), request, response);
        var html = org.jsoup.Jsoup.parse(response.getContentAsString());
        var socialPanel = html.selectFirst(".ygl-login-panel--social");
        var emailPanel = html.selectFirst(".ygl-login-panel--email");

        assertThat(socialPanel).isNotNull();
        assertThat(socialPanel.select("a.ygl-provider-sign-in"))
                .extracting(button -> button.text()).containsExactly("Continue with Google", "Continue with X");
        assertThat(socialPanel.text()).contains("Already have an account?")
                .contains("link Google and X from Sign-in methods");
        assertThat(emailPanel).isNotNull();
        assertThat(emailPanel.select("form[action='/register']")).hasSize(1);
        assertThat(html.selectFirst(".ygl-login-footer a[href='/login']")).isNotNull();
        assertThat(html.selectFirst(".ygl-login-footer").elementSiblingIndex())
                .isGreaterThan(emailPanel.elementSiblingIndex());
        assertThat(html.selectFirst(".ygl-auth-transition")).isNull();
    }

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
