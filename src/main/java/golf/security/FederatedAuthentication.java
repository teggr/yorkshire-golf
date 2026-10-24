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
import org.springframework.security.oauth2.client.web.*;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import java.time.Instant;
import java.util.List;

@Configuration(proxyBeanMethods = false)
public class FederatedAuthentication {
    private static final String CALLBACK_FLOW = FederatedAuthentication.class.getName();
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
        var registrations = federatedClientRegistrations();
        var delegate = new DefaultOAuth2AuthorizationRequestResolver(registrations, "/oauth2/authorization");
        delegate.setAuthorizationRequestCustomizer(builder -> builder.additionalParameters(parameters -> {
            // Explicit query mode keeps Apple's identity-only response on a Lax-cookie top-level GET.
            parameters.put("response_mode", "query");
        }));
        OAuth2AuthorizationRequestResolver resolver = new OAuth2AuthorizationRequestResolver() {
            @Override public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
                return customize(request, delegate.resolve(request));
            }
            @Override public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String id) {
                return customize(request, delegate.resolve(request, id));
            }
        };
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
                    if ("facebook".equals(provider)) parameters.put("auth_type", "reauthenticate");
                    else if ("google".equals(provider)) {
                        parameters.put("prompt", "select_account");
                        parameters.put("max_age", "0");
                    }
                });
        if ("google".equals(provider)) OAuth2AuthorizationRequestCustomizers.withPkce().accept(builder);
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
                    if (principal instanceof OidcUser oidc) {
                        Instant authenticatedAt = oidc.getAuthenticatedAt();
                        // Apple has no reliable max_age/auth_time support. Sensitive callbacks follow
                        // explicit existing-method confirmation and require a fresh nonce-bound token.
                        if ("apple".equals(provider) && authenticatedAt == null) authenticatedAt = oidc.getIssuedAt();
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
                    user = identities.login(provider, subject);
                }
                var local = UsernamePasswordAuthenticationToken.authenticated(new LocalPrincipal(user.id()), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.role())));
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(local);
                SecurityContextHolder.setContext(context);
                new HttpSessionSecurityContextRepository().saveContext(context, request, response);
                // No provider user, claims, ID/access/refresh token, or authorized client survives this request.
                response.sendRedirect("/challenge");
            } catch (RuntimeException ex) {
                failure().onAuthenticationFailure(request, response, invalid());
            }
        };
    }

    AuthenticationFailureHandler failure() {
        return (request, response, exception) -> {
            SecurityContextHolder.clearContext();
            var session = request.getSession(false);
            if (session != null) session.invalidate();
            response.sendRedirect("/login?error=true");
        };
    }

    private static OAuth2AuthenticationException invalid() {
        return new OAuth2AuthenticationException(new OAuth2Error("authentication_failed"));
    }
}
