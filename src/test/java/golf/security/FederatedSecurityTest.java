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
    void csrfProtectsManagementAndApplePostCallback() throws Exception {
        mvc.perform(post("/account/link/google").with(user("user@example.com")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/login/oauth2/code/apple").param("code", "unsolicited").param("state", "bad"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/account")).andExpect(status().is3xxRedirection());
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

    private GolfUser local() {
        return users.save(new GolfUser(null, UUID.randomUUID() + "@example.com",
                passwords.encode("correct-password"), UUID.randomUUID().toString(), "USER", false, 0));
    }
}
