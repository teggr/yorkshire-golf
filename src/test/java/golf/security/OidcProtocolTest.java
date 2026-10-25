package golf.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;
import golf.GolfTrackerApplication;
import golf.user.UserRepository;
import golf.user.ExternalIdentityService;
import golf.user.GolfUser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {GolfTrackerApplication.class, OidcProtocolTest.ProviderConfiguration.class},
        properties = {"spring.datasource.url=jdbc:h2:mem:oidc-protocol;DB_CLOSE_DELAY=-1", "spring.mail.host=localhost"})
@AutoConfigureMockMvc
class OidcProtocolTest {
    private static final RSAKey KEY;
    private static final HttpServer SERVER;
    private static final String ISSUER;
    private static volatile String idToken;
    private static volatile String facebookSubject;
    private static volatile String facebookAuthorization;
    private static volatile String facebookQuery;
    private static volatile String xSubject;
    private static volatile String xAuthorization;
    private static volatile String xQuery;
    private static volatile String tokenRequestBody;
    private static volatile String tokenAuthorization;
    static {
        try {
            KEY = new RSAKeyGenerator(2048).keyID("test-key").generate();
            SERVER = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            ISSUER = "http://127.0.0.1:" + SERVER.getAddress().getPort();
            SERVER.createContext("/jwks", exchange -> {
                byte[] bytes = new JWKSet(KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            SERVER.createContext("/token", exchange -> {
                tokenAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
                tokenRequestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                byte[] bytes = ("{\"access_token\":\"test-access-token-do-not-save\",\"token_type\":\"Bearer\","
                        + "\"expires_in\":3600,\"id_token\":\"" + idToken + "\"}").getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            SERVER.createContext("/me", exchange -> {
                facebookAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
                facebookQuery = exchange.getRequestURI().getRawQuery();
                byte[] bytes = ("{\"id\":\"" + facebookSubject
                        + "\",\"email\":\"ignored-provider-email@example.com\",\"name\":\"Ignored name\"}")
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            SERVER.createContext("/x-me", exchange -> {
                xAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
                xQuery = exchange.getRequestURI().getRawQuery();
                byte[] bytes = ("{\"data\":{\"id\":\"" + xSubject
                        + "\",\"name\":\"Ignored X name\",\"username\":\"ignored_x_username\"}}")
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            SERVER.start();
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProviderConfiguration {
        @Bean @Primary
        FederatedProviders testProviders() {
            return new FederatedProviders(new MockEnvironment()
                    .withProperty("golf.app.base-url", "https://golf.example")
                    .withProperty("golf.auth.google.enabled", "true")
                    .withProperty("golf.auth.google.client-id", "test-client")
                    .withProperty("golf.auth.google.client-secret", "test-secret")
                    .withProperty("golf.auth.apple.enabled", "true")
                    .withProperty("golf.auth.apple.client-id", "test-client")
                    .withProperty("golf.auth.apple.client-secret", FederatedProvidersTest.appleSecret())
                    .withProperty("golf.auth.facebook.enabled", "true")
                    .withProperty("golf.auth.facebook.client-id", "test-client")
                    .withProperty("golf.auth.facebook.client-secret", "test-secret")
                    .withProperty("golf.auth.x.enabled", "true")
                    .withProperty("golf.auth.x.client-id", "test-client")
                    .withProperty("golf.auth.x.client-secret", "test-secret")) {
                @Override public ClientRegistration registration(String provider) {
                    ClientRegistration original = super.registration(provider);
                    if (original == null) return null;
                    return ClientRegistration.withClientRegistration(original)
                            .authorizationUri(ISSUER + "/authorize")
                            .tokenUri(ISSUER + "/token")
                            .jwkSetUri(ISSUER + "/jwks")
                            .userInfoUri(switch (provider) {
                                case "facebook" -> ISSUER + "/me?fields=id";
                                case "x" -> ISSUER + "/x-me?user.fields=id";
                                default -> null;
                            })
                            .userNameAttributeName("x".equals(provider) ? "data" : original.getProviderDetails()
                                    .getUserInfoEndpoint().getUserNameAttributeName())
                            .issuerUri(ISSUER).build();
                }
            };
        }
    }

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired ExternalIdentityService identities;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder passwords;

    @AfterAll static void stopProvider() { SERVER.stop(0); }

    @Test
    void realSignedOidcCallbackRotatesSessionAndKeepsOnlyLocalPrincipal() throws Exception {
        var start = start();
        String oldSessionId = start.session().getId();
        idToken = token(start.parameters().get("nonce"), ISSUER, "test-client", 600, KEY);
        var result = mvc.perform(get("/login/oauth2/code/google").session(start.session())
                        .param("state", start.parameters().get("state")).param("code", "test-code"))
                .andExpect(redirectedUrl("/challenge")).andReturn();
        var session = result.getRequest().getSession(false);
        Map<String, String> tokenParameters = parseParameters(tokenRequestBody);
        String verifier = tokenParameters.get("code_verifier");
        assertThat(verifier).isNotBlank();
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                java.security.MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        assertThat(start.parameters().get("code_challenge")).isEqualTo(challenge);
        assertThat(session.getId()).isNotEqualTo(oldSessionId);
        SecurityContext context = (SecurityContext) session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(context.getAuthentication().getPrincipal()).isInstanceOf(LocalPrincipal.class);
        assertThat(context.toString()).doesNotContain("test-access-token", idToken, "provider-subject", "provider-email");
        for (String name : Collections.list(session.getAttributeNames())) {
            assertThat(session.getAttribute(name).toString()).doesNotContain("test-access-token", idToken, "provider-email");
        }
    }

    @Test
    void googleSignInWithVerifiedEmailForExistingPasswordAccountDoesNotCreateAnotherAccount() throws Exception {
        GolfUser existing = local();
        int before = users.findAll().size();
        var start = start();
        assertThat(start.parameters().get("scope")).isEqualTo("openid email");
        String subject = "google-" + UUID.randomUUID();
        var original = SignedJWT.parse(token(start.parameters().get("nonce"), ISSUER, "test-client", 600, KEY, subject));
        var jwt = new SignedJWT(original.getHeader(), new JWTClaimsSet.Builder(original.getJWTClaimsSet())
                .claim("email", existing.email()).claim("email_verified", true).build());
        jwt.sign(new RSASSASigner(KEY));
        idToken = jwt.serialize();

        mvc.perform(get("/login/oauth2/code/google").session(start.session())
                        .param("state", start.parameters().get("state")).param("code", "test-code"))
                .andExpect(redirectedUrl("/login?existing=true"));

        assertThat(users.findAll()).hasSize(before);
        assertThat(identities.find("google", subject)).isEmpty();
        mvc.perform(get("/login").param("existing", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Sign in with your existing method first, then go to Sign-in methods and link Google")));
    }

    @Test
    void rejectsWrongNonceIssuerAudienceExpiredAndInvalidSignatureWithoutCreatingAccounts() throws Exception {
        for (String invalid : List.of("nonce", "issuer", "audience", "expired", "signature")) {
            int before = users.findAll().size();
            var start = start();
            idToken = token(invalid.equals("nonce") ? "wrong-nonce" : start.parameters().get("nonce"),
                    invalid.equals("issuer") ? "https://wrong-issuer.example" : ISSUER,
                    invalid.equals("audience") ? "wrong-client" : "test-client",
                    invalid.equals("expired") ? -600 : 600,
                    invalid.equals("signature") ? new RSAKeyGenerator(2048).keyID("test-key").generate() : KEY);
            mvc.perform(get("/login/oauth2/code/google").session(start.session())
                            .param("state", start.parameters().get("state")).param("code", "test-code"))
                    .andExpect(redirectedUrl("/login?error=true"));
            assertThat(users.findAll()).hasSize(before);
        }
    }

    @Test
    void rejectsTamperedStateBeforeTokenExchange() throws Exception {
        var start = start();
        mvc.perform(get("/login/oauth2/code/google").session(start.session())
                        .param("state", start.parameters().get("state") + "tampered").param("code", "test-code"))
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    void providerCancellationFailsGenericallyWithoutCreatingAnAccount() throws Exception {
        var start = start();
        int before = users.findAll().size();
        mvc.perform(get("/login/oauth2/code/google").session(start.session())
                        .param("state", start.parameters().get("state")).param("error", "access_denied"))
                .andExpect(redirectedUrl("/login?error=true"));
        assertThat(users.findAll()).hasSize(before);
        assertThat(start.session().isInvalid()).isTrue();
    }

    @Test
    void providerCancellationDuringLinkingKeepsTheExistingAccountSession() throws Exception {
        GolfUser user = local();
        var start = startAction(user, "link", "password");
        var result = mvc.perform(get("/login/oauth2/code/google").session(start.session())
                        .param("state", start.parameters().get("state")).param("error", "access_denied"))
                .andExpect(redirectedUrl("/account?error=true")).andReturn();
        assertThat(start.session().isInvalid()).isFalse();
        assertThat(identities.list(user.id())).isEmpty();
        assertThat(result.getRequest().getSession().getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).isNotNull();
        mvc.perform(get("/account").session(start.session()))
                .andExpect(status().isOk());
    }

    @Test
    void expiredAuthorizationRequestFailsBeforeTokenExchange() throws Exception {
        var start = start();
        int before = users.findAll().size();
        var request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setSession(start.session());
        request.setParameter("state", start.parameters().get("state"));
        var repository = new org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository();
        var authorization = repository.loadAuthorizationRequest(request);
        var expired = org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest.from(authorization)
                .attributes(attributes -> attributes.put("golf.expires", Instant.now().minusSeconds(1))).build();
        repository.saveAuthorizationRequest(expired, request, new org.springframework.mock.web.MockHttpServletResponse());
        mvc.perform(get("/login/oauth2/code/google").session(start.session())
                        .param("state", start.parameters().get("state")).param("code", "test-code"))
                .andExpect(redirectedUrl("/login?error=true"));
        assertThat(users.findAll()).hasSize(before);
        assertThat(start.session().isInvalid()).isTrue();
    }

    @Test
    void appleIdentityOnlyUsesFormPostCallbackAndValidatesOidcToken() throws Exception {
        var result = mvc.perform(get("/oauth2/authorization/apple")).andReturn();
        assertThat(result.getResponse().getRedirectedUrl()).contains("scope=openid", "response_mode=form_post")
                .doesNotContain("scope=email", "code_challenge", "prompt=", "max_age=");
        var start = parseStart(result);
        idToken = token(start.parameters().get("nonce"), ISSUER, "test-client", 600, KEY);
        mvc.perform(post("/login/oauth2/code/apple").session(start.session())
                        .param("state", start.parameters().get("state")).param("code", "test-code"))
                .andExpect(redirectedUrl("/challenge"));
    }

    @Test
    void facebookOAuthUsesOnlyAppScopedIdAndNeverPersistsReturnedEmailOrName() throws Exception {
        facebookSubject = "facebook-" + UUID.randomUUID();
        var result = mvc.perform(get("/oauth2/authorization/facebook")).andReturn();
        assertThat(result.getResponse().getRedirectedUrl()).contains("scope=public_profile", "auth_type=reauthenticate")
                .doesNotContain("email", "code_challenge");
        var start = parseStart(result);
        var callback = mvc.perform(get("/login/oauth2/code/facebook").session(start.session())
                        .param("state", start.parameters().get("state")).param("code", "test-code"))
                .andExpect(redirectedUrl("/challenge")).andReturn();
        GolfUser user = users.findById(identities.find("facebook", facebookSubject).orElseThrow().userId()).orElseThrow();
        assertThat(facebookAuthorization).isEqualTo("Bearer " + "test-access-token-do-not-save");
        assertThat(facebookQuery).isEqualTo("fields=id");
        assertThat(user.email()).isNull();
        assertThat(user.password()).isNull();
        for (String name : Collections.list(callback.getRequest().getSession().getAttributeNames())) {
            assertThat(callback.getRequest().getSession().getAttribute(name).toString())
                    .doesNotContain("ignored-provider-email", "Ignored name", "test-access-token");
        }
    }

    @Test
    void xOAuthUsesPkceAndOnlyPersistsTheReturnedAccountId() throws Exception {
        xSubject = "x-" + UUID.randomUUID();
        var result = mvc.perform(get("/oauth2/authorization/x")).andReturn();
        assertThat(result.getResponse().getRedirectedUrl())
                .contains("scope=users.read", "code_challenge=", "code_challenge_method=S256")
                .doesNotContain("email", "tweet.read");
        var start = parseStart(result);
        var callback = mvc.perform(get("/login/oauth2/code/x").session(start.session())
                        .param("state", start.parameters().get("state")).param("code", "test-code"))
                .andReturn();
        assertThat(xAuthorization).as("X user-info authorization").isEqualTo("Bearer test-access-token-do-not-save");
        assertThat(xQuery).as("X user-info fields").isEqualTo("user.fields=id");
        assertThat(callback.getResponse().getRedirectedUrl()).isEqualTo("/challenge");
        assertThat(tokenAuthorization).isEqualTo("Basic " + Base64.getEncoder()
                .encodeToString("test-client:test-secret".getBytes(StandardCharsets.UTF_8)));
        assertThat(tokenRequestBody).contains("code_verifier=");
        GolfUser user = users.findById(identities.find("x", xSubject).orElseThrow().userId()).orElseThrow();
        assertThat(tokenRequestBody).contains("code_verifier=");
        assertThat(user.email()).isNull();
        assertThat(user.password()).isNull();
        for (String name : Collections.list(callback.getRequest().getSession().getAttributeNames())) {
            assertThat(callback.getRequest().getSession().getAttribute(name).toString())
                    .doesNotContain("Ignored X name", "ignored_x_username", "test-access-token");
        }
    }

    @Test
    void explicitLinkUsesExistingLocalAccountAndRejectsProviderIdentityOwnedElsewhere() throws Exception {
        GolfUser user = local();
        int before = users.findAll().size();
        var start = startAction(user, "link", "password");
        String subject = "link-" + UUID.randomUUID();
        idToken = token(start.parameters().get("nonce"), ISSUER, "test-client", 600, KEY, subject);
        mvc.perform(get("/login/oauth2/code/google").session(start.session())
                        .param("state", start.parameters().get("state")).param("code", "test-code"))
                .andExpect(redirectedUrl("/account?success=linked"));
        assertThat(identities.find("google", subject).orElseThrow().userId()).isEqualTo(user.id());
        assertThat(users.findAll()).hasSize(before);
        assertThat(users.findById(user.id()).orElseThrow().trackerId()).isEqualTo(user.trackerId());

        GolfUser attacker = local();
        var otherStart = startAction(attacker, "link", "password");
        idToken = token(otherStart.parameters().get("nonce"), ISSUER, "test-client", 600, KEY, subject);
        mvc.perform(get("/login/oauth2/code/google").session(otherStart.session())
                        .param("state", otherStart.parameters().get("state")).param("code", "test-code"))
                .andExpect(redirectedUrl("/account?error=true"));
        assertThat(identities.find("google", subject).orElseThrow().userId()).isEqualTo(user.id());
    }

    @Test
    void externalOnlyReauthenticationMustMatchExistingIdentityAndGrantsOneUseConfirmation() throws Exception {
        String subject = "reauth-" + UUID.randomUUID();
        GolfUser user = identities.login("google", subject);
        var start = startAction(user, "reauthenticate", null);
        idToken = token(start.parameters().get("nonce"), ISSUER, "test-client", 600, KEY, subject);
        var result = mvc.perform(get("/login/oauth2/code/google").session(start.session())
                        .param("state", start.parameters().get("state")).param("code", "test-code"))
                .andExpect(redirectedUrl("/account?success=reauthenticated")).andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession();
        identities.link(user.id(), "facebook", "facebook-" + UUID.randomUUID());
        mvc.perform(post("/account/unlink/facebook").session(session).with(csrf()).param("confirmed", "true"))
                .andExpect(redirectedUrl("/account?success=unlinked"));
        assertThat(identities.list(user.id())).extracting(ExternalIdentityService.Identity::provider).containsExactly("google");
        mvc.perform(post("/account/unlink/google").session(session).with(csrf()).param("confirmed", "true"))
                .andExpect(redirectedUrl("/account?error=true"));

        var wrong = startAction(user, "reauthenticate", null);
        idToken = token(wrong.parameters().get("nonce"), ISSUER, "test-client", 600, KEY, "different-provider-account");
        mvc.perform(get("/login/oauth2/code/google").session(wrong.session())
                        .param("state", wrong.parameters().get("state")).param("code", "test-code"))
                .andExpect(redirectedUrl("/account?error=true"));
    }

    @Test
    void appleLinkingUsesPriorPasswordConfirmationWithoutUnsupportedAuthTime() throws Exception {
        String subject = "apple-link-" + UUID.randomUUID();
        GolfUser user = local();
        var start = startAction(user, "link", "password", "apple");
        String original = token(start.parameters().get("nonce"), ISSUER, "test-client", 600, KEY, subject);
        var parsed = SignedJWT.parse(original);
        var claims = new JWTClaimsSet.Builder(parsed.getJWTClaimsSet()).claim("auth_time", null)
                .issueTime(Date.from(Instant.now())).build();
        var jwt = new SignedJWT(parsed.getHeader(), claims);
        jwt.sign(new RSASSASigner(KEY));
        idToken = jwt.serialize();
        mvc.perform(post("/login/oauth2/code/apple").session(start.session())
                        .param("state", start.parameters().get("state")).param("code", "test-code"))
                .andExpect(redirectedUrl("/account?success=linked"));
        assertThat(identities.find("apple", subject).orElseThrow().userId()).isEqualTo(user.id());
    }

    @Test
    void appleOnlyConfirmationIsExplicitCsrfProtectedAndSingleUse() throws Exception {
        String subject = UUID.randomUUID().toString();
        GolfUser user = identities.login("apple", subject);
        var session = new MockHttpSession();
        mvc.perform(post("/account/link/google").session(session).with(user(new LocalPrincipal(user.id())))
                        .with(csrf()).param("confirmed", "true"))
                .andExpect(redirectedUrl("/account?error=true"));
        mvc.perform(post("/account/reauthenticate/apple").session(session)
                        .param("confirmed", "true"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/account/reauthenticate/apple").session(session).with(csrf()))
                .andExpect(redirectedUrl("/account?error=true"));
        mvc.perform(post("/account/reauthenticate/apple").session(session).with(csrf())
                        .param("confirmed", "true"))
                .andExpect(redirectedUrl("/oauth2/authorization/apple"));
        mvc.perform(post("/account/link/google").session(session).with(csrf()).param("confirmed", "true"))
                .andExpect(redirectedUrl("/account?error=true"));
        var start = parseStart(mvc.perform(get("/oauth2/authorization/apple").session(session)).andReturn());
        var original = SignedJWT.parse(token(start.parameters().get("nonce"), ISSUER, "test-client", 600, KEY, subject));
        var jwt = new SignedJWT(original.getHeader(), new JWTClaimsSet.Builder(original.getJWTClaimsSet())
                .claim("auth_time", null).issueTime(Date.from(Instant.now().minusSeconds(3600))).build());
        jwt.sign(new RSASSASigner(KEY));
        idToken = jwt.serialize();
        mvc.perform(post("/login/oauth2/code/apple").session(start.session())
                        .param("state", start.parameters().get("state")).param("code", "test-code"))
                .andExpect(redirectedUrl("/account?success=confirmed"));
        mvc.perform(post("/account/link/google").with(user(new LocalPrincipal(user.id()))).with(csrf())
                        .param("confirmed", "true"))
                .andExpect(redirectedUrl("/account?error=true"));
        mvc.perform(post("/account/link/google").session(session).with(csrf()).param("confirmed", "true"))
                .andExpect(redirectedUrl("/oauth2/authorization/google"));
        mvc.perform(post("/account/link/facebook").session(session).with(csrf()).param("confirmed", "true"))
                .andExpect(redirectedUrl("/account?error=true"));
    }

    @Test
    void stolenApplicationSessionCannotConfirmUsingAttackersAppleIdentity() throws Exception {
        GolfUser owner = identities.login("apple", UUID.randomUUID().toString());
        String attackerSubject = UUID.randomUUID().toString();
        identities.login("apple", attackerSubject);
        int before = users.findAll().size();
        var start = startAction(owner, "reauthenticate", null, "apple");
        idToken = token(start.parameters().get("nonce"), ISSUER, "test-client", 600, KEY, attackerSubject);
        mvc.perform(post("/login/oauth2/code/apple").session(start.session())
                        .param("state", start.parameters().get("state")).param("code", "test-code"))
                .andExpect(redirectedUrl("/account?error=true"));
        assertThat(start.session().isInvalid()).isFalse();
        assertThat(users.findAll()).hasSize(before);
        mvc.perform(post("/account/link/google").session(start.session()).with(csrf())
                        .param("confirmed", "true"))
                .andExpect(redirectedUrl("/account?error=true"));
    }

    @Test
    void appleConfirmationCannotBypassAnotherSupportedReauthenticationMethod() throws Exception {
        GolfUser user = identities.login("apple", UUID.randomUUID().toString());
        identities.link(user.id(), "google", UUID.randomUUID().toString());
        mvc.perform(post("/account/reauthenticate/apple").with(user(new LocalPrincipal(user.id())))
                        .with(csrf()).param("confirmed", "true"))
                .andExpect(redirectedUrl("/account?error=true"));
    }

    @Test
    void onlyAppleCallbackPostIsCsrfExemptAndStillRequiresValidState() throws Exception {
        mvc.perform(post("/login/oauth2/code/apple").param("state", "invalid").param("code", "test-code"))
                .andExpect(redirectedUrl("/login?error=true"));
        mvc.perform(post("/login/oauth2/code/google").param("state", "invalid").param("code", "test-code"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/account/link/apple").with(user("user@example.com")).param("confirmed", "true"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/login/oauth2/code/apple/extra").param("state", "invalid"))
                .andExpect(status().isForbidden());
    }

    @Test
    void appleFormPostCancellationRequiresStateAndNeverCreatesAnAccount() throws Exception {
        var start = parseStart(mvc.perform(get("/oauth2/authorization/apple")).andReturn());
        int before = users.findAll().size();
        mvc.perform(post("/login/oauth2/code/apple").session(start.session())
                        .param("state", start.parameters().get("state")).param("error", "access_denied"))
                .andExpect(redirectedUrl("/login?error=true"));
        assertThat(users.findAll()).hasSize(before);
    }

    @Test
    void providerButtonsUseLocalBrandAssetsAndAccessibleProviderLabels() throws Exception {
        var result = mvc.perform(get("/login")).andExpect(status().isOk()).andReturn();
        var page = org.jsoup.Jsoup.parse(result.getResponse().getContentAsString());
        assertThat(page.selectFirst(".ygl-auth-transition").text())
                .contains("A note from", "@yorkshiregolflife", "keep your data safe",
                        "existing members", "Sign-in methods", "New accounts must use");
        assertThat(page.selectFirst(".ygl-auth-transition a[href='https://www.instagram.com/yorkshiregolflife/']"))
                .isNotNull();
        for (String provider : List.of("google", "apple", "facebook", "x")) {
            String label = "Continue with " + FederatedProviders.label(provider);
            var button = page.selectFirst("a[href='/oauth2/authorization/" + provider + "']");
            assertThat(button).isNotNull();
            assertThat(button.text()).isEqualTo(label);
            assertThat(button.attr("aria-label")).isEqualTo(label);
            assertThat(button.hasClass("ygl-provider-sign-in--" + provider)).isTrue();
            assertThat(button.attr("hx-boost")).isEqualTo("false");
            var logo = button.selectFirst("img");
            assertThat(logo).isNotNull();
            assertThat(logo.attr("src")).isEqualTo("/images/auth/" + provider + ".svg");
            assertThat(logo.attr("alt")).isEmpty();
            assertThat(logo.attr("aria-hidden")).isEqualTo("true");
            mvc.perform(get(logo.attr("src"))).andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("image/svg+xml"))
                    .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script"))));
        }
        assertThat(page.select("script[src*=accounts.google.com], script[src*=connect.facebook.net], script[src*=appleid.cdn-apple.com]"))
                .isEmpty();
        mvc.perform(get("/css/styles.css")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(".ygl-provider-sign-in:focus-visible")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(".ygl-provider-sign-in--google")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(".ygl-provider-sign-in--apple")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(".ygl-provider-sign-in--facebook")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(".ygl-provider-sign-in--x")));
    }

    @Test
    void registrationRequiresAnEnabledFederatedProvider() throws Exception {
        var result = mvc.perform(get("/register")).andExpect(status().isOk()).andReturn();
        var page = org.jsoup.Jsoup.parse(result.getResponse().getContentAsString());
        assertThat(page.text()).contains("New accounts must be created using one of these sign-in providers");
        assertThat(page.select("a[href='/oauth2/authorization/x']")).hasSize(1);
        assertThat(page.select("form[action='/register']")).isEmpty();
        assertThat(page.select("input[type=email]")).isEmpty();
    }

    @Test
    void linkingAcceptsFreshGoogleAuthorizationWhenAuthTimeIsStale() throws Exception {
        GolfUser user = local();
        var start = startAction(user, "link", "password");
        var original = SignedJWT.parse(token(start.parameters().get("nonce"), ISSUER, "test-client", 600, KEY));
        var jwt = new SignedJWT(original.getHeader(), new JWTClaimsSet.Builder(original.getJWTClaimsSet())
                .claim("auth_time", Instant.now().minusSeconds(600).getEpochSecond()).build());
        jwt.sign(new RSASSASigner(KEY));
        idToken = jwt.serialize();
        var result = mvc.perform(get("/login/oauth2/code/google").session(start.session())
                        .param("state", start.parameters().get("state")).param("code", "test-code"))
                .andExpect(redirectedUrl("/account?success=linked")).andReturn();
        assertThat(identities.find("google", SignedJWT.parse(idToken).getJWTClaimsSet().getSubject())
                .orElseThrow().userId()).isEqualTo(user.id());
        assertThat(start.session().isInvalid()).isFalse();
        assertThat(result.getRequest().getSession().getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).isNotNull();
        mvc.perform(get("/account").session(start.session()))
                .andExpect(status().isOk());
    }

    @Test
    void googleReauthenticationStillRequiresRecentAuthenticationTime() throws Exception {
        GolfUser user = local();
        String subject = "reauth-" + UUID.randomUUID();
        identities.link(user.id(), "google", subject);
        var start = startAction(user, "reauthenticate", null);
        var original = SignedJWT.parse(token(start.parameters().get("nonce"), ISSUER, "test-client", 600, KEY, subject));
        var jwt = new SignedJWT(original.getHeader(), new JWTClaimsSet.Builder(original.getJWTClaimsSet())
                .claim("auth_time", Instant.now().minusSeconds(600).getEpochSecond()).build());
        jwt.sign(new RSASSASigner(KEY));
        idToken = jwt.serialize();
        mvc.perform(get("/login/oauth2/code/google").session(start.session())
                        .param("state", start.parameters().get("state")).param("code", "test-code"))
                .andExpect(redirectedUrl("/account?error=true"));
        assertThat(identities.find("google", subject).orElseThrow().userId()).isEqualTo(user.id());
    }

    @Test
    void linkCannotBeReboundToDifferentUserOrExpiredFlow() throws Exception {
        for (boolean expired : List.of(true, false)) {
            GolfUser user = local();
            var start = startAction(user, "link", "password");
            start.session().setAttribute(FederatedFlow.SESSION_KEY,
                    new FederatedFlow("link", "google", expired ? user.id() : local().id(),
                            expired ? Instant.now().minusSeconds(1) : Instant.now().plusSeconds(300)));
            int before = users.findAll().size();
            mvc.perform(get("/login/oauth2/code/google").session(start.session())
                            .param("state", start.parameters().get("state")).param("code", "test-code"))
                    .andExpect(redirectedUrl("/login?error=true"));
            assertThat(users.findAll()).hasSize(before);
        }
    }

    private GolfUser local() {
        return users.save(new GolfUser(null, UUID.randomUUID() + "@example.com", passwords.encode("password"),
                UUID.randomUUID().toString(), "USER", false, 0));
    }

    private Start startAction(GolfUser user, String action, String password) throws Exception {
        return startAction(user, action, password, "google");
    }

    private Start startAction(GolfUser user, String action, String password, String provider) throws Exception {
        var post = post("/account/" + action + "/" + provider).with(user(new LocalPrincipal(user.id())))
                .with(csrf()).param("confirmed", "true");
        if (password != null) post.param("password", password);
        var result = mvc.perform(post).andExpect(redirectedUrl("/oauth2/authorization/" + provider)).andReturn();
        var redirected = mvc.perform(get("/oauth2/authorization/" + provider)
                        .session((MockHttpSession) result.getRequest().getSession()))
                .andExpect(status().is3xxRedirection()).andReturn();
        return parseStart(redirected);
    }

    private record Start(MockHttpSession session, Map<String, String> parameters) {}
    private Start start() throws Exception {
        var result = mvc.perform(get("/oauth2/authorization/google")).andExpect(status().is3xxRedirection()).andReturn();
        return parseStart(result);
    }

    private Start parseStart(org.springframework.test.web.servlet.MvcResult result) {
        URI redirect = URI.create(result.getResponse().getRedirectedUrl());
        return new Start((MockHttpSession) result.getRequest().getSession(), parseParameters(redirect.getRawQuery()));
    }

    private Map<String, String> parseParameters(String encoded) {
        Map<String, String> params = new HashMap<>();
        for (String pair : encoded.split("&")) {
            String[] parts = pair.split("=", 2);
            params.put(parts[0], URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
        }
        return params;
    }

    private String token(String nonce, String issuer, String audience, int lifetime, RSAKey signingKey) throws Exception {
        return token(nonce, issuer, audience, lifetime, signingKey, "provider-subject-" + UUID.randomUUID());
    }

    private String token(String nonce, String issuer, String audience, int lifetime, RSAKey signingKey, String subject) throws Exception {
        Instant now = Instant.now();
        var claims = new JWTClaimsSet.Builder().issuer(issuer).audience(audience)
                .subject(subject).issueTime(Date.from(now.minusSeconds(1000)))
                .expirationTime(Date.from(now.plusSeconds(lifetime))).claim("nonce", nonce)
                .claim("email", "provider-email@example.com").claim("auth_time", now.getEpochSecond()).build();
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key").build(), claims);
        jwt.sign(new RSASSASigner(signingKey));
        return jwt.serialize();
    }

}
