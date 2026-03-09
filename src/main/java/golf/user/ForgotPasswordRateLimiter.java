package golf.user;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ForgotPasswordRateLimiter {

    static final int MAX_REQUESTS_PER_WINDOW = 3;
    static final Duration WINDOW = Duration.ofHours(1);

    private final Clock clock;
    private final Map<String, Deque<Instant>> requestsByEmail = new ConcurrentHashMap<>();

    public ForgotPasswordRateLimiter() {
        this(Clock.systemUTC());
    }

    ForgotPasswordRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean allow(String email) {
        String normalizedEmail = normalize(email);
        if (normalizedEmail == null) {
            return false;
        }

        Instant now = Instant.now(clock);
        Instant cutoff = now.minus(WINDOW);

        Deque<Instant> deque = requestsByEmail.computeIfAbsent(normalizedEmail, key -> new ArrayDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
                deque.removeFirst();
            }

            if (deque.size() >= MAX_REQUESTS_PER_WINDOW) {
                return false;
            }

            deque.addLast(now);
            return true;
        }
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
}
