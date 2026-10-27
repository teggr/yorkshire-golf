package golf.security;

import golf.GolfTrackerApplication;
import golf.user.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = GolfTrackerApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:federated-security;DB_CLOSE_DELAY=-1",
        "spring.mail.host=localhost",
        "golf.auth.google.enabled=true", "golf.auth.google.client-id=test-client",
        "golf.auth.google.client-secret=test-secret", "golf.app.base-url=https://golf.example"})
@AutoConfigureMockMvc
class FederatedSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired ExternalIdentityService identities;
    @Autowired FederatedAuthentication federated;
    @Autowired PasswordEncoder passwords;
    @Autowired UserRoundRepository rounds;
    @Autowired PasswordResetTokenRepository resetTokens;
    @Autowired AccountActionGuard accountActionGuard;
    @Autowired ForgotPasswordRateLimiter forgotPasswordRateLimiter;

    @Test
    void loginListsOnlyEnabledProvidersAndUsesFixedCallbackDespiteHostHeader() throws Exception {
        mvc.perform(get("/login")).andExpect(content().string(org.hamcrest.Matchers.containsString("Continue with Google")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Continue with Apple"))));
        var result = mvc.perform(get("/oauth2/authorization/google").header("Host", "attacker.example"))
                .andExpect(status().is3xxRedirection()).andReturn();
        assertThat(result.getResponse().getRedirectedUrl()).contains("redirect_uri=https://golf.example/login/oauth2/code/google")
                .contains("scope=openid", "prompt=select_account", "max_age=0", "code_challenge=", "code_challenge_method=S256")
                .contains("nonce=").contains("state=").doesNotContain("attacker.example");
    }

    @Test
    void xSignInExplainsHowExistingUsersShouldAvoidCreatingADuplicateAccount() throws Exception {
        var providers = new FederatedProviders(new org.springframework.mock.env.MockEnvironment()
                .withProperty("golf.app.base-url", "https://golf.example")
                .withProperty("golf.auth.x.enabled", "true")
                .withProperty("golf.auth.x.client-id", "test-client")
                .withProperty("golf.auth.x.client-secret", "test-secret"));
        var response = new org.springframework.mock.web.MockHttpServletResponse();
        new LoginPage(providers).render(Map.of(), new org.springframework.mock.web.MockHttpServletRequest("GET", "/login"),
                response);
        var page = org.jsoup.Jsoup.parse(response.getContentAsString());
        var xButton = page.selectFirst("a.ygl-provider-sign-in--x");

        assertThat(xButton).isNotNull();
        assertThat(xButton.text()).isEqualTo("Continue with X");
        assertThat(page.selectFirst(".ygl-auth-transition").text()).contains("X")
                .doesNotContain("Google", "Facebook", "Apple");
        assertThat(page.selectFirst(".ygl-existing-account-hint").text())
                .contains("link X from Sign-in methods to avoid creating a separate account");
        assertThat(page.select(".ygl-login-panel")).hasSize(2);
        assertThat(page.selectFirst(".ygl-login-panel--social").select("a.ygl-provider-sign-in"))
                .hasSize(1);
        assertThat(page.selectFirst(".ygl-login-panel--email").select("form[action='/login']"))
                .hasSize(1);
        assertThat(page.selectFirst(".ygl-login-footer a[href='/register']")).isNotNull();
        assertThat(page.selectFirst(".ygl-login-footer").elementSiblingIndex())
                .isGreaterThan(page.selectFirst(".ygl-login-panel--email").elementSiblingIndex());
    }

    @Test
    void csrfProtectsManagementAndApplePostCallback() throws Exception {
        mvc.perform(post("/account/link/google").with(user("user@example.com")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/account/delete").with(user("user@example.com")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/login/oauth2/code/apple").param("code", "unsolicited").param("state", "bad"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/account")).andExpect(status().is3xxRedirection());
    }

    @Test
    @org.junit.jupiter.api.extension.ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
    void federatedFailureLogsProviderAndCodeWithoutExceptionDetails(
            org.springframework.boot.test.system.CapturedOutput output) throws Exception {
            var request = new org.springframework.mock.web.MockHttpServletRequest("GET", "/login/oauth2/code/google");
            request.setRequestURI("/login/oauth2/code/google");
            var response = new org.springframework.mock.web.MockHttpServletResponse();
            String responseBody = "{\"type\":\"https://api.x.com/2/problems/client-forbidden\","
                    + "\"detail\":\"sensitive-response-body\"}";
            var responseException = org.springframework.web.client.HttpClientErrorException.create(
                    org.springframework.http.HttpStatus.FORBIDDEN, "forbidden",
                    org.springframework.http.HttpHeaders.EMPTY, responseBody.getBytes(), null);
        var exception = new org.springframework.security.oauth2.core.OAuth2AuthenticationException(
                new org.springframework.security.oauth2.core.OAuth2Error("invalid_user_info_response"),
                "sensitive-detail", responseException);

        federated.failure().onAuthenticationFailure(request, response, exception);

        assertThat(response.getRedirectedUrl()).isEqualTo("/login?error=true");
        assertThat(output).contains("provider google", "invalid_user_info_response", "HTTP 403", "client-forbidden")
                .doesNotContain("sensitive-detail", "sensitive-response-body");
    }

    @Test
    void managementRequiresPasswordConfirmationAndExplicitPostBeforeProviderRedirect() throws Exception {
        GolfUser user = local();
        var principal = new LocalPrincipal(user.id());
        mvc.perform(get("/oauth2/authorization/google").with(user(principal))).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
        mvc.perform(post("/account/link/google").with(user(principal)).with(csrf())
                        .param("confirmed", "true").param("password", "wrong"))
                .andExpect(redirectedUrl("/account?error=true"));
        var result = mvc.perform(post("/account/link/google").with(user(principal)).with(csrf())
                        .param("confirmed", "true").param("password", "correct-password"))
                .andExpect(redirectedUrl("/oauth2/authorization/google")).andReturn();
        assertThat(result.getRequest().getSession().getAttribute(FederatedFlow.SESSION_KEY)).isInstanceOf(FederatedFlow.class);
    }

    @Test
    void successReplacesAllProviderClaimsWithLocalIdAndPersistsNoAuthorizedClient() throws Exception {
        String subject = UUID.randomUUID().toString();
        var claims = Map.<String, Object>of("id", subject, "email", "do-not-store@example.com", "name", "Do not store");
        var remote = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("OAUTH2_USER")), claims, "id");
        var oauth = new OAuth2AuthenticationToken(remote, remote.getAuthorities(), "facebook");
        var request = new org.springframework.mock.web.MockHttpServletRequest();
        var response = new org.springframework.mock.web.MockHttpServletResponse();
        request.setSession(new MockHttpSession());
        federated.success().onAuthenticationSuccess(request, response, oauth);
        SecurityContext context = (SecurityContext) request.getSession().getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(context.getAuthentication().getPrincipal()).isInstanceOf(LocalPrincipal.class);
        assertThat(context.getAuthentication().getCredentials()).isNull();
        assertThat(context.toString()).doesNotContain(subject, "do-not-store", "OAUTH2_USER");
        assertThat((Object) federated.transientAuthorizedClients().loadAuthorizedClient("facebook", oauth, request)).isNull();
        assertThat(response.getRedirectedUrl()).isEqualTo("/challenge");
        SecurityContextHolder.clearContext();
    }

    @Test
    void invalidStateAndUnsolicitedCallbacksFailGenericallyWithoutCreatingAccount() throws Exception {
        int before = users.findAll().size();
        mvc.perform(get("/login/oauth2/code/google").param("code", "code").param("state", "invalid"))
                .andExpect(redirectedUrl("/login?error=true"));
        assertThat(users.findAll()).hasSize(before);
    }

    @Test
    void localIdPrincipalResolvesChallengeTrackerAndManagement() throws Exception {
        GolfUser user = local();
        mvc.perform(get("/challenge").with(user(new LocalPrincipal(user.id()))))
                .andExpect(redirectedUrl("/challenge/" + user.trackerId()));
        mvc.perform(get("/account").with(user(new LocalPrincipal(user.id()))))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Email and password")));
    }

    @Test
    void googleOnlyAccountShowsLinkedProviderInSiteStyleWithoutUnsafeActions() throws Exception {
        GolfUser user = identities.login("google", UUID.randomUUID().toString());
        var result = mvc.perform(get("/account").with(user(new LocalPrincipal(user.id()))))
                .andExpect(status().isOk()).andReturn();
        var page = org.jsoup.Jsoup.parse(result.getResponse().getContentAsString());
        assertThat(page.selectFirst(".ygl-page-header h1").text()).isEqualTo("Sign-in methods");
        assertThat(page.select(".ygl-sign-in-method-card")).hasSize(1);
        assertThat(page.select(".ygl-sign-in-method-card").text()).contains("Google", "Linked");
        assertThat(page.select("form[action='/account/unlink/google'], form[action='/account/link/google']")).isEmpty();
        assertThat(page.select("form[action='/account/reauthenticate/google']")).hasSize(1);
        assertThat(page.select("form[action='/account/delete']")).hasSize(1);
        assertThat(page.selectFirst(".ygl-sign-in-methods__notice").text())
                .contains("only sign-in method", "cannot be unlinked");
    }

    @Test
    void accountActionsAreGroupedIntoProviderAndDeletionSections() throws Exception {
        GolfUser user = local();
        var result = mvc.perform(get("/account").with(user(new LocalPrincipal(user.id()))))
                .andExpect(status().isOk()).andReturn();
        var page = org.jsoup.Jsoup.parse(result.getResponse().getContentAsString());

        var linkForm = page.selectFirst(".ygl-sign-in-methods__available form.ygl-sign-in-action");
        assertThat(page.select(".ygl-sign-in-methods__available form.ygl-sign-in-action"))
                .extracting(form -> form.attr("action")).containsExactly("/account/link/google");
        assertThat(linkForm.selectFirst("button.ygl-provider-sign-in--google").text()).isEqualTo("Link Google");
        assertThat(linkForm.selectFirst("button.ygl-provider-sign-in--google img").attr("src"))
                .isEqualTo("/images/auth/google.svg");
        assertThat(page.selectFirst(".ygl-sign-in-methods__danger form[action='/account/delete']"))
                .isNotNull();
        assertThat(page.select(".ygl-sign-in-methods__danger h2").text()).isEqualTo("Delete account");
    }

    @Test
    void adminCanIdentifyExternalAccountsWithoutEmail() throws Exception {
        GolfUser external = identities.login("google", UUID.randomUUID().toString());
        mvc.perform(get("/admin").with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("External account #" + external.id())));
    }

    @Test
    void passwordLoginAndLogoutStillWorkWithFederatedLoginEnabled() throws Exception {
        GolfUser user = local();
        var result = mvc.perform(post("/login").with(csrf()).param("username", user.email())
                        .param("password", "correct-password"))
                .andExpect(redirectedUrl("/challenge")).andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession();
        mvc.perform(get("/challenge").session(session))
                .andExpect(redirectedUrl("/challenge/" + user.trackerId()));
        mvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(redirectedUrl("/"));
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void federatedPrincipalCanOnlyWriteOwnTracker() throws Exception {
        GolfUser owner = identities.login("google", UUID.randomUUID().toString());
        GolfUser other = identities.login("google", UUID.randomUUID().toString());
        mvc.perform(post("/challenge/" + owner.trackerId() + "/add-round")
                        .with(user(new LocalPrincipal(other.id()))).with(csrf())
                        .param("courseName", "Alwoodley").param("date", "2026-10-24"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/challenge/" + owner.trackerId() + "/add-round")
                        .with(user(new LocalPrincipal(owner.id()))).with(csrf())
                        .param("courseName", "Alwoodley").param("date", "2026-10-24"))
                .andExpect(redirectedUrl("/challenge/" + owner.trackerId() + "?added=true"));
    }

    @Test
    void accountShowsClearSuccessMessagesForEachSensitiveAction() throws Exception {
        GolfUser user = local();
        for (var message : Map.of(
                "linked", "Sign-in method linked successfully.",
                "unlinked", "Sign-in method unlinked successfully.",
                "confirmed", "Account change confirmed for this session. You can now finish one change within five minutes.",
                "reauthenticated", "Identity confirmed. You can now change a sign-in method or delete your account.").entrySet()) {
            mvc.perform(get("/account").with(user(new LocalPrincipal(user.id()))).param("success", message.getKey()))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString(message.getValue())));
        }
    }

    @Test
    void accountPageExplainsDeletionAndRequiresRecentProviderConfirmation() throws Exception {
        GolfUser user = identities.login("google", "google-" + UUID.randomUUID());
        var result = mvc.perform(get("/account").with(user(new LocalPrincipal(user.id()))))
                .andExpect(status().isOk()).andReturn();
        var page = org.jsoup.Jsoup.parse(result.getResponse().getContentAsString());

        assertThat(page.select("form[action='/account/delete']")).hasSize(1);
        assertThat(page.select("form[action='/account/delete']").text())
                .contains("I understand this permanently deletes my account and all golf rounds");
        assertThat(page.select("form[action='/account/reauthenticate/google']")).hasSize(1);
        assertThat(page.text()).contains("Confirm your identity with one of your linked sign-in providers");
    }

    @Test
    void accountDeletionRequiresPasswordConfirmationAndDeletesAccountData() throws Exception {
        GolfUser user = local();
        identities.link(user.id(), "facebook", "facebook-" + UUID.randomUUID());
        rounds.save(user.id(), "Alwoodley", "2026-10-24");
        resetTokens.save(user.id(), "reset-token-for-deletion");
        for (int i = 0; i < 3; i++) {
            assertThat(forgotPasswordRateLimiter.allow(user.email())).isTrue();
        }
        assertThat(forgotPasswordRateLimiter.allow(user.email())).isFalse();

        mvc.perform(post("/account/delete").with(user(new LocalPrincipal(user.id()))).with(csrf())
                        .param("confirmed", "true").param("password", "wrong-password"))
                .andExpect(redirectedUrl("/account?error=true"));
        assertThat(users.findById(user.id())).isPresent();
        assertThat(identities.list(user.id())).hasSize(1);
        assertThat(rounds.findByUserId(user.id())).hasSize(1);
        assertThat(resetTokens.findByToken("reset-token-for-deletion")).isPresent();

        var result = mvc.perform(post("/account/delete").with(user(new LocalPrincipal(user.id()))).with(csrf())
                        .param("confirmed", "true").param("password", "correct-password"))
                .andExpect(redirectedUrl("/login?deleted=true")).andReturn();

        assertThat(users.findById(user.id())).isEmpty();
        assertThat(identities.list(user.id())).isEmpty();
        assertThat(rounds.findByUserId(user.id())).isEmpty();
        assertThat(resetTokens.findByToken("reset-token-for-deletion")).isEmpty();
        assertThat(forgotPasswordRateLimiter.allow(user.email())).isTrue();
        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    void passwordlessAccountDeletionRequiresRecentProviderConfirmation() throws Exception {
        GolfUser user = identities.login("google", "google-" + UUID.randomUUID());
        MockHttpSession session = new MockHttpSession();

        mvc.perform(post("/account/delete").session(session).with(user(new LocalPrincipal(user.id()))).with(csrf())
                        .param("confirmed", "true"))
                .andExpect(redirectedUrl("/account?error=true"));
        assertThat(users.findById(user.id())).isPresent();

        accountActionGuard.confirmed(user.id(), session);
        mvc.perform(post("/account/delete").session(session).with(user(new LocalPrincipal(user.id()))).with(csrf())
                        .param("confirmed", "true"))
                .andExpect(redirectedUrl("/login?deleted=true"));
        assertThat(users.findById(user.id())).isEmpty();
    }

    @Test
    void loginConfirmsWhenAccountWasDeleted() throws Exception {
        mvc.perform(get("/login").param("deleted", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Your account and golf rounds have been deleted.")));
    }

    private GolfUser local() {
        return users.save(new GolfUser(null, UUID.randomUUID() + "@example.com",
                passwords.encode("correct-password"), UUID.randomUUID().toString(), "USER", false, 0));
    }
}
