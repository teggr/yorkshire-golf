package golf.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.*;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FederatedProvidersTest {
    @Test
    void providersAreDisabledByDefault() {
        assertThat(new FederatedProviders(new MockEnvironment()).enabled()).isEmpty();
    }

    @Test
    void explicitlyDisabledProviderIsNotRegisteredEvenWithCredentials() {
        var providers = new FederatedProviders(new MockEnvironment()
                .withProperty("golf.auth.facebook.enabled", "false")
                .withProperty("golf.auth.facebook.client-id", "client")
                .withProperty("golf.auth.facebook.client-secret", "secret"));

        assertThat(providers.enabled()).doesNotContain("facebook");
        assertThat(providers.registration("facebook")).isNull();
    }

    @Test
    void enablingProviderWithoutCredentialsFailsClearly() {
        assertThatThrownBy(() -> new FederatedProviders(new MockEnvironment()
                .withProperty("golf.auth.google.enabled", "true")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("google");
    }

    @Test
    void rejectsInsecurePublicOriginsAndNonOriginUrls() {
        for (String base : new String[]{"http://example.com", "https://example.com/path", "******example.com",
                "https://example.com?query", "https://example.com#fragment"}) {
            assertThatThrownBy(() -> new FederatedProviders(environment(base)))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void fixedCallbacksAndMinimumScopes() {
        FederatedProviders providers = new FederatedProviders(environment("https://golf.example"));
        var google = providers.registration("google");
        assertThat(google.getRedirectUri()).isEqualTo("https://golf.example/login/oauth2/code/google");
        assertThat(google.getScopes()).containsExactly("openid", "email");
        assertThat(google.getProviderDetails().getIssuerUri()).isEqualTo("https://accounts.google.com");
    }

    @Test
    void xUsesOAuth2WithOnlyTheIdentityScope() {
        var providers = new FederatedProviders(environment("https://golf.example")
                .withProperty("golf.auth.x.enabled", "true")
                .withProperty("golf.auth.x.client-id", "x-client")
                .withProperty("golf.auth.x.client-secret", "x-secret"));
        var x = providers.registration("x");
        assertThat(x.getRedirectUri()).isEqualTo("https://golf.example/login/oauth2/code/x");
        assertThat(x.getProviderDetails().getAuthorizationUri()).isEqualTo("https://twitter.com/i/oauth2/authorize");
        assertThat(x.getProviderDetails().getTokenUri()).isEqualTo("https://api.twitter.com/2/oauth2/token");
        assertThat(x.getScopes()).containsExactly("users.read");
        assertThat(x.getClientAuthenticationMethod()).isEqualTo(
                org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
        assertThat(x.getProviderDetails().getUserInfoEndpoint().getUri())
                .isEqualTo("https://api.twitter.com/2/users/me?user.fields=id");
        assertThat(x.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName()).isEqualTo("data");
        assertThat(providers.enabled()).contains("x");
        assertThat(FederatedProviders.label("x")).isEqualTo("X");
    }

    @Test
    void httpsDeploymentForcesSecureHttpOnlySessionCookie() {
        var factory = mock(org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory.class);
        var settings = new org.springframework.boot.web.server.servlet.ServletWebServerSettings();
        settings.setSession(new org.springframework.boot.web.server.servlet.Session());
        when(factory.getSettings()).thenReturn(settings);
        new AuthenticationCookies().authenticationCookieCustomizer(
                new FederatedProviders(environment("https://golf.example"))).customize(factory);
        assertThat(settings.getSession().getCookie().getSecure()).isTrue();
        assertThat(settings.getSession().getCookie().getHttpOnly()).isTrue();
        assertThat(settings.getSession().getCookie().getSameSite()).isEqualTo(org.springframework.boot.web.server.Cookie.SameSite.LAX);
    }

    @Test
    void appleRequiresValidExpiringSecretAndOnlyRequestsIdentity() {
        var environment = new MockEnvironment().withProperty("golf.app.base-url", "https://golf.example")
                .withProperty("golf.auth.apple.enabled", "true")
                .withProperty("golf.auth.apple.client-id", "test-client")
                .withProperty("golf.auth.apple.client-secret", "not-a-jwt");
        assertThatThrownBy(() -> new FederatedProviders(environment)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("apple").hasMessageNotContaining("not-a-jwt");
        var providers = new FederatedProviders(environment.withProperty("golf.auth.apple.client-secret", appleSecret()));
        assertThat(providers.enabled()).containsExactly("apple");
        assertThat(providers.registration("apple").getScopes()).containsExactly("openid");
        assertThat(providers.registration("apple").getProviderDetails().getIssuerUri()).isEqualTo("https://appleid.apple.com");
        assertThat(providers.registration("apple").getProviderDetails().getUserInfoEndpoint().getUri()).isNull();
        var factory = mock(org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory.class);
        var settings = new org.springframework.boot.web.server.servlet.ServletWebServerSettings();
        settings.setSession(new org.springframework.boot.web.server.servlet.Session());
        when(factory.getSettings()).thenReturn(settings);
        new AuthenticationCookies().authenticationCookieCustomizer(providers).customize(factory);
        assertThat(settings.getSession().getCookie().getSameSite()).isEqualTo(org.springframework.boot.web.server.Cookie.SameSite.NONE);
        assertThat(settings.getSession().getCookie().getSecure()).isTrue();
        for (String origin : new String[]{"http://localhost:8080", "https://localhost", "https://127.0.0.1",
                "https://192.168.1.1", "https://[::1]", "https://dev.local"}) {
            assertThatThrownBy(() -> new FederatedProviders(environment.withProperty("golf.app.base-url", origin)))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("Apple");
        }
    }

    private MockEnvironment environment(String base) {
        return new MockEnvironment().withProperty("golf.app.base-url", base)
                .withProperty("golf.auth.google.enabled", "true")
                .withProperty("golf.auth.google.client-id", "client")
                .withProperty("golf.auth.google.client-secret", "secret");
    }

    static String appleSecret() {
        try {
            var key = new ECKeyGenerator(Curve.P_256).keyID("test-apple-key").generate();
            var now = Instant.now();
            var claims = new JWTClaimsSet.Builder().issuer("test-apple-team").subject("test-client")
                    .audience("https://appleid.apple.com").issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(3600))).build();
            var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(key.getKeyID()).build(), claims);
            jwt.sign(new ECDSASigner(key));
            return jwt.serialize();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
