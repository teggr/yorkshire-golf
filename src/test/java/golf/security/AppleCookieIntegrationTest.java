package golf.security;

import golf.GolfTrackerApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = GolfTrackerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.datasource.url=jdbc:h2:mem:apple-cookie;DB_CLOSE_DELAY=-1",
                "spring.mail.host=localhost", "golf.app.base-url=https://golf.example",
                "golf.auth.apple.enabled=true", "golf.auth.apple.client-id=test-client",
                "server.servlet.session.cookie.same-site=lax"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AppleCookieIntegrationTest {
    @LocalServerPort int port;

    @DynamicPropertySource
    static void appleSecret(DynamicPropertyRegistry registry) {
        registry.add("golf.auth.apple.client-secret", FederatedProvidersTest::appleSecret);
    }

    @Test
    void realSessionCookieOverridesLaxDefaultForAppleFormPost() throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/oauth2/authorization/apple")).GET().build();
        var response = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()
                .send(request, HttpResponse.BodyHandlers.discarding());
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().allValues("set-cookie")).anySatisfy(cookie ->
                assertThat(cookie).contains("JSESSIONID=", "Secure", "HttpOnly", "SameSite=None"));
        assertThat(response.headers().firstValue("location").orElseThrow()).contains("response_mode=form_post");
    }
}
