package golf.security;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    static final int MAX_FAILURES = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final Clock clock;
    private final Map<String, AttemptState> attemptsByEmail = new ConcurrentHashMap<>();

    public LoginAttemptService() {
        this(Clock.systemUTC());
    }

    LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    public void recordFailure(String email) {
        String key = normalize(email);
        if (key == null) {
            return;
        }

        attemptsByEmail.compute(key, (ignored, state) -> {
            AttemptState current = state == null ? new AttemptState() : state;
            Instant now = Instant.now(clock);

            if (current.lockedUntil != null && now.isAfter(current.lockedUntil)) {
                current = new AttemptState();
            }

            if (current.lockedUntil != null && now.isBefore(current.lockedUntil)) {
                return current;
            }

            current.failures++;
            if (current.failures >= MAX_FAILURES) {
                current.lockedUntil = now.plus(LOCK_DURATION);
                current.failures = 0;
            }
            return current;
        });
    }

    public void recordSuccess(String email) {
        String key = normalize(email);
        if (key == null) {
            return;
        }
        attemptsByEmail.remove(key);
    }

    public boolean isLocked(String email) {
        String key = normalize(email);
        if (key == null) {
            return false;
        }

        AttemptState state = attemptsByEmail.get(key);
        if (state == null || state.lockedUntil == null) {
            return false;
        }

        Instant now = Instant.now(clock);
        if (!now.isBefore(state.lockedUntil)) {
            attemptsByEmail.remove(key);
            return false;
        }
        return true;
    }

    private String normalize(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private static final class AttemptState {
        private int failures;
        private Instant lockedUntil;
    }
}
