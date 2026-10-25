package golf.security;

import java.io.Serializable;
import java.time.Instant;

public record FederatedFlow(String action, String provider, Long userId, Instant expiresAt) implements Serializable {
    public static final String SESSION_KEY = FederatedFlow.class.getName();
    public boolean valid() { return Instant.now().isBefore(expiresAt); }
}
