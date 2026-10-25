package golf.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;

@Component
public class LocalRegistration {
    private final boolean enabled;

    public LocalRegistration(
            @Value("${golf.auth.local-registration.enabled:false}") boolean enabled,
            @Value("${golf.app.base-url:http://localhost:8080}") String baseUrl) {
        this.enabled = enabled;
        if (enabled && !isLoopbackOrigin(baseUrl)) {
            throw new IllegalStateException("Local email/password registration requires APP_BASE_URL to use HTTP loopback");
        }
    }

    public boolean enabled() {
        return enabled;
    }

    private static boolean isLoopbackOrigin(String baseUrl) {
        try {
            URI uri = URI.create(baseUrl);
            return "http".equals(uri.getScheme())
                    && List.of("localhost", "127.0.0.1", "[::1]").contains(uri.getHost())
                    && uri.getUserInfo() == null && uri.getRawQuery() == null && uri.getRawFragment() == null
                    && uri.getPath().isEmpty();
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
