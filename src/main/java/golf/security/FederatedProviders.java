package golf.security;

import com.nimbusds.jwt.SignedJWT;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class FederatedProviders {
    private final Map<String, ClientRegistration> registrations = new LinkedHashMap<>();
    private final String baseUrl;

    public FederatedProviders(Environment environment) {
        baseUrl = environment.getProperty("golf.app.base-url", "http://localhost:8080").replaceAll("/$", "");
        for (String provider : List.of("google", "apple", "facebook")) {
            if (!environment.getProperty("golf.auth." + provider + ".enabled", Boolean.class, false)) continue;
            validateOrigin(baseUrl);
            String prefix = "golf.auth." + provider;
            String id = required(environment, prefix + ".client-id", provider);
            String secret = required(environment, prefix + ".client-secret", provider);
            ClientRegistration.Builder builder = ClientRegistration.withRegistrationId(provider)
                    .clientId(id).clientSecret(secret).clientName(label(provider))
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .clientSettings(ClientRegistration.ClientSettings.builder()
                            .requireProofKey("google".equals(provider)).build())
                    .redirectUri(baseUrl + "/login/oauth2/code/" + provider);
            switch (provider) {
                case "google" -> builder.scope("openid")
                        .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                        .tokenUri("https://oauth2.googleapis.com/token")
                        .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                        .issuerUri("https://accounts.google.com")
                        .userNameAttributeName("sub");
                case "apple" -> {
                    validateAppleOrigin(baseUrl);
                    validateAppleSecret(secret, id);
                    builder.scope("openid")
                            .authorizationUri("https://appleid.apple.com/auth/authorize")
                            .tokenUri("https://appleid.apple.com/auth/token")
                            .jwkSetUri("https://appleid.apple.com/auth/keys")
                            .issuerUri("https://appleid.apple.com")
                            .userNameAttributeName("sub");
                }
                case "facebook" -> builder.scope("public_profile")
                        .authorizationUri("https://www.facebook.com/v23.0/dialog/oauth")
                        .tokenUri("https://graph.facebook.com/v23.0/oauth/access_token")
                        .userInfoUri("https://graph.facebook.com/v23.0/me?fields=id")
                        .userInfoAuthenticationMethod(org.springframework.security.oauth2.core.AuthenticationMethod.HEADER)
                        .userNameAttributeName("id");
                default -> throw new IllegalStateException();
            }
            registrations.put(provider, builder.build());
        }
    }

    public List<String> enabled() { return List.copyOf(registrations.keySet()); }
    public ClientRegistration registration(String provider) { return registrations.get(provider); }
    public boolean secureCookies() { return baseUrl.startsWith("https://"); }
    public static String label(String provider) {
        return switch (provider) {
            case "google" -> "Google";
            case "apple" -> "Apple";
            case "facebook" -> "Facebook";
            default -> throw new IllegalArgumentException("Unknown provider");
        };
    }

    private static String required(Environment environment, String key, String provider) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("Enabled " + provider + " requires " + key);
        return value;
    }

    private static void validateOrigin(String base) {
        try {
            URI uri = URI.create(base);
            boolean loopback = List.of("localhost", "127.0.0.1", "[::1]").contains(uri.getHost());
            if (uri.getHost() == null || uri.getUserInfo() != null || uri.getRawQuery() != null
                    || uri.getRawFragment() != null || !uri.getPath().isEmpty()
                    || !("https".equals(uri.getScheme()) || loopback && "http".equals(uri.getScheme()))) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Enabled authentication requires APP_BASE_URL to be an HTTPS origin (HTTP loopback only)", ex);
        }
    }

    private static void validateAppleSecret(String secret, String clientId) {
        try {
            SignedJWT jwt = SignedJWT.parse(secret);
            var claims = jwt.getJWTClaimsSet();
            Instant now = Instant.now();
            if (!"ES256".equals(jwt.getHeader().getAlgorithm().getName()) || jwt.getHeader().getKeyID() == null
                    || claims.getIssuer() == null || !clientId.equals(claims.getSubject())
                    || !claims.getAudience().equals(List.of("https://appleid.apple.com"))
                    || claims.getExpirationTime() == null || claims.getIssueTime() == null
                    || !claims.getExpirationTime().toInstant().isAfter(now.plusSeconds(60))
                    || claims.getExpirationTime().toInstant().isAfter(now.plusSeconds(15777000))) {
                throw new IllegalArgumentException();
            }

        } catch (Exception ex) {
            // Never include the configured secret (or parser exception) in startup diagnostics.
            throw new IllegalStateException("Enabled apple requires an unexpired ES256 Apple client-secret JWT with kid, iss, sub, aud, iat and exp");
        }
    }

    private static void validateAppleOrigin(String base) {
        try {
            URI uri = URI.create(base);
            String host = uri.getHost().toLowerCase(java.util.Locale.ROOT);
            if (!"https".equals(uri.getScheme()) || "localhost".equals(host)
                    || host.endsWith(".localhost") || host.endsWith(".local")
                    || !host.contains(".") && !host.contains(":")) throw new IllegalArgumentException();
            if (host.matches("[0-9.]+") || host.contains(":")) {
                var address = java.net.InetAddress.getByName(host);
                if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress() || host.matches("\\[?f[cd].*")) {
                    throw new IllegalArgumentException();
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Apple authentication requires APP_BASE_URL to be a public HTTPS origin");
        }
    }
}
