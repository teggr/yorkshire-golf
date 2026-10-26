package golf.security;

import golf.user.ExternalIdentityService;
import golf.user.GolfUser;
import golf.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.web.*;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

@Configuration(proxyBeanMethods = false)
public class FederatedAuthentication {
    private static final String CALLBACK_FLOW = FederatedAuthentication.class.getName();
    private static final Logger log = LoggerFactory.getLogger(FederatedAuthentication.class);
    private final FederatedProviders providers;
    private final ExternalIdentityService identities;
    private final UserService users;
    private final AccountActionGuard guard;

    public FederatedAuthentication(FederatedProviders providers, ExternalIdentityService identities,
                                   UserService users, AccountActionGuard guard) {
        this.providers = providers;
        this.identities = identities;
        this.users = users;
        this.guard = guard;
    }

    @Bean
    ClientRegistrationRepository federatedClientRegistrations() {
        return providers::registration;
    }

    @Bean
    OAuth2AuthorizedClientRepository transientAuthorizedClients() {
        return new OAuth2AuthorizedClientRepository() {
            @Override public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(
                    String id, Authentication authentication, HttpServletRequest request) { return null; }
            @Override public void saveAuthorizedClient(OAuth2AuthorizedClient client, Authentication authentication,
                    HttpServletRequest request, HttpServletResponse response) {}
            @Override public void removeAuthorizedClient(String id, Authentication authentication,
                    HttpServletRequest request, HttpServletResponse response) {}
        };
    }

    public void configure(HttpSecurity http) throws Exception {
        if (providers.enabled().isEmpty()) return;
        if (providers.enabled().contains("apple")) {
            http.csrf(csrf -> csrf.ignoringRequestMatchers(
                    org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.withDefaults()
                            .matcher(org.springframework.http.HttpMethod.POST, "/login/oauth2/code/apple")));
        }
        var registrations = federatedClientRegistrations();
        var delegate = new DefaultOAuth2AuthorizationRequestResolver(registrations, "/oauth2/authorization");
        OAuth2AuthorizationRequestResolver resolver = new OAuth2AuthorizationRequestResolver() {
            @Override public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
                return customize(request, delegate.resolve(request));
            }
            @Override public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String id) {
                return customize(request, delegate.resolve(request, id));
            }
        };
        var oauth2UserService = new DefaultOAuth2UserService();
        http.oauth2Login(oauth -> oauth
                .loginPage("/login")
                .withObjectPostProcessor(new org.springframework.security.config.ObjectPostProcessor<OAuth2AuthorizationRequestRedirectFilter>() {
                    @Override public <O extends OAuth2AuthorizationRequestRedirectFilter> O postProcess(O filter) {
                        filter.setAuthenticationFailureHandler(failure());
                        return filter;
                    }
                })
                .clientRegistrationRepository(registrations)
                .authorizedClientRepository(transientAuthorizedClients())
                .authorizationEndpoint(endpoint -> endpoint
                        .authorizationRequestResolver(resolver)
                        .authorizationRequestRepository(authorizationRequests()))
                .userInfoEndpoint(endpoint -> endpoint.userService(request -> {
                    OAuth2User remote = oauth2UserService.loadUser(request);
                    if (!"x".equals(request.getClientRegistration().getRegistrationId())) return remote;
                    Object profile = remote.getAttribute("data");
                    if (!(profile instanceof java.util.Map<?, ?> attributes)
                            || !(attributes.get("id") instanceof String subject) || subject.isBlank()) {
                        throw invalid();
                    }
                    return new DefaultOAuth2User(remote.getAuthorities(), java.util.Map.of("id", subject), "id");
                }))
                .successHandler(success())
                .failureHandler(failure()));
    }

    private OAuth2AuthorizationRequest customize(HttpServletRequest request, OAuth2AuthorizationRequest authorization) {
        if (authorization == null) return null;
        String provider = authorization.getAttribute("registration_id");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean signedIn = authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        var session = request.getSession();
        Object pending = session.getAttribute(FederatedFlow.SESSION_KEY);
        if (signedIn) {
            GolfUser current = users.findByPrincipal(authentication).orElseThrow(FederatedAuthentication::invalid);
            if (!(pending instanceof FederatedFlow flow) || !flow.valid() || !flow.userId().equals(current.id())
                    || !flow.provider().equals(provider)) throw invalid();
        } else {
            session.removeAttribute(FederatedFlow.SESSION_KEY);
        }
        var builder = OAuth2AuthorizationRequest.from(authorization)
                .attributes(attributes -> {
                    attributes.put("golf.expires", Instant.now().plusSeconds(300));
                    if (signedIn) attributes.put("golf.flow", pending);
                })
                .additionalParameters(parameters -> {
                    parameters.put("response_mode", "apple".equals(provider) ? "form_post" : "query");
                    if ("facebook".equals(provider)) parameters.put("auth_type", "reauthenticate");
                    else if ("google".equals(provider)) {
                        parameters.put("prompt", "select_account");
                        parameters.put("max_age", "0");
                    }
                });
        if ("google".equals(provider) || "x".equals(provider)) {
            OAuth2AuthorizationRequestCustomizers.withPkce().accept(builder);
        }
        return builder.build();
    }

    AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequests() {
        var delegate = new HttpSessionOAuth2AuthorizationRequestRepository();
        return new AuthorizationRequestRepository<>() {
            @Override public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
                return delegate.loadAuthorizationRequest(request);
            }
            @Override public void saveAuthorizationRequest(OAuth2AuthorizationRequest value,
                    HttpServletRequest request, HttpServletResponse response) {
                delegate.saveAuthorizationRequest(value, request, response);
            }
            @Override public OAuth2AuthorizationRequest removeAuthorizationRequest(
                    HttpServletRequest request, HttpServletResponse response) {
                OAuth2AuthorizationRequest value = delegate.removeAuthorizationRequest(request, response);
                if (value == null) return null;
                Instant expires = value.getAttribute("golf.expires");
                if (expires == null || !Instant.now().isBefore(expires)) throw invalid();
                var session = request.getSession(false);
                Object pending = session.getAttribute(FederatedFlow.SESSION_KEY);
                session.removeAttribute(FederatedFlow.SESSION_KEY);
                if (!java.util.Objects.equals(pending, value.getAttribute("golf.flow"))) throw invalid();
                if (pending instanceof FederatedFlow flow) {
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    GolfUser user = users.findByPrincipal(authentication).orElseThrow(FederatedAuthentication::invalid);
                    if (!flow.valid() || !flow.userId().equals(user.id())
                            || !flow.provider().equals(value.getAttribute("registration_id"))) throw invalid();
                    request.setAttribute(CALLBACK_FLOW, flow);
                } else {
                    Authentication current = SecurityContextHolder.getContext().getAuthentication();
                    if (current != null && current.isAuthenticated()
                            && !(current instanceof AnonymousAuthenticationToken)) throw invalid();
                }
                return value;
            }
        };
    }

    AuthenticationSuccessHandler success() {
        return (request, response, authentication) -> {
            try {
                OAuth2AuthenticationToken oauth = (OAuth2AuthenticationToken) authentication;
                String provider = oauth.getAuthorizedClientRegistrationId();
                OAuth2User principal = oauth.getPrincipal();
                String subject = principal instanceof OidcUser oidc ? oidc.getSubject() : principal.getAttribute("id");
                GolfUser user;
                Object pending = request.getAttribute(CALLBACK_FLOW);
                if (pending instanceof FederatedFlow flow) {
                    if (!flow.valid() || !provider.equals(flow.provider())) throw invalid();
                    if ("reauthenticate".equals(flow.action()) && "google".equals(provider)
                            && principal instanceof OidcUser oidc) {
                        Instant authenticatedAt = oidc.getAuthenticatedAt();
                        if (authenticatedAt == null || authenticatedAt.isBefore(flow.expiresAt().minusSeconds(360))) {
                            throw invalid();
                        }
                    }
                    if ("link".equals(flow.action())) {
                        user = identities.link(flow.userId(), provider, subject);
                    } else {
                        var identity = identities.find(provider, subject).orElseThrow(FederatedAuthentication::invalid);
                        if (!identity.userId().equals(flow.userId())) throw invalid();
                        user = identities.usable(flow.userId());
                        guard.confirmed(user.id(), request.getSession());
                    }
                } else {
                    String email = null;
                    if ("google".equals(provider) && principal instanceof OidcUser oidc
                            && Boolean.TRUE.equals(oidc.getClaims().get("email_verified"))) {
                        email = oidc.getClaimAsString("email");
                    } else if ("facebook".equals(provider)) {
                        email = principal.getAttribute("email");
                    }
                    if (email != null && identities.find(provider, subject).isEmpty()
                            && users.findByEmail(email).isPresent()) {
                        log.info("Blocked a new {} identity because its provider email is already registered", provider);
                        SecurityContextHolder.clearContext();
                        var session = request.getSession(false);
                        if (session != null) session.invalidate();
                        response.sendRedirect("/login?existing=" + provider);
                        return;
                    }
                    user = identities.login(provider, subject);
                }
                var local = UsernamePasswordAuthenticationToken.authenticated(new LocalPrincipal(user.id()), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.role())));
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(local);
                SecurityContextHolder.setContext(context);
                new HttpSessionSecurityContextRepository().saveContext(context, request, response);
                // No provider user, claims, ID/access/refresh token, or authorized client survives this request.
                response.sendRedirect(pending instanceof FederatedFlow flow
                        ? "/account?success=" + ("link".equals(flow.action()) ? "linked"
                                : "apple".equals(provider) ? "confirmed" : "reauthenticated")
                        : "/challenge");
            } catch (RuntimeException ex) {
                log.warn("Federated authentication callback processing failed", ex);
                failure().onAuthenticationFailure(request, response, invalid());
            }
        };
    }

    AuthenticationFailureHandler failure() {
        return (request, response, exception) -> {
            if (restoreAccountAfterFailedFlow(request, response)) return;
            SecurityContextHolder.clearContext();
            var session = request.getSession(false);
            if (session != null) session.invalidate();
            response.sendRedirect("/login?error=true");
        };
    }

    private boolean restoreAccountAfterFailedFlow(HttpServletRequest request, HttpServletResponse response)
            throws java.io.IOException {
        if (!(request.getAttribute(CALLBACK_FLOW) instanceof FederatedFlow flow) || !flow.valid()) return false;
        var session = request.getSession(false);
        var user = users.findById(flow.userId()).filter(account -> !account.accountLocked()).orElse(null);
        if (session == null || user == null) return false;

        session.removeAttribute(FederatedFlow.SESSION_KEY);
        var local = UsernamePasswordAuthenticationToken.authenticated(new LocalPrincipal(user.id()), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.role())));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(local);
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);
        response.sendRedirect("/account?error=true");
        return true;
    }

    private static OAuth2AuthenticationException invalid() {
        return new OAuth2AuthenticationException(new OAuth2Error("authentication_failed"));
    }
}
