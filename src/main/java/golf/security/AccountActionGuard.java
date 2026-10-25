package golf.security;

import golf.user.ExternalIdentityService;
import golf.user.GolfUser;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AccountActionGuard {
    private record Attempts(int count, Instant reset) {}
    private record Recent(Long userId, Instant until) implements java.io.Serializable {}
    private final ConcurrentHashMap<Long, Attempts> attempts = new ConcurrentHashMap<>();
    private final PasswordEncoder passwords;
    private final ExternalIdentityService identities;
    private static final String RECENT = AccountActionGuard.class.getName();

    public AccountActionGuard(PasswordEncoder passwords, ExternalIdentityService identities) {
        this.passwords = passwords;
        this.identities = identities;
    }

    public void rateLimit(Long userId) {
        Instant now = Instant.now();
        if (attempts.size() > 10000) attempts.entrySet().removeIf(entry -> entry.getValue().reset().isBefore(now));
        Attempts result = attempts.compute(userId, (id, old) ->
                old == null || old.reset().isBefore(now) ? new Attempts(1, now.plusSeconds(300))
                        : new Attempts(old.count() + 1, old.reset()));
        if (result.count() > 10) throw new IllegalArgumentException("Please try later");
    }

    public void requireConfirmation(GolfUser user, String password, HttpSession session) {
        identities.usable(user.id());
        if (user.password() != null) {
            if (password == null || !passwords.matches(password, user.password())) {
                throw new IllegalArgumentException("Confirmation failed");
            }
        } else {
            Object value = session.getAttribute(RECENT);
            session.removeAttribute(RECENT);
            if (!(value instanceof Recent recent) || !recent.userId().equals(user.id())
                    || !Instant.now().isBefore(recent.until())) throw new IllegalArgumentException("Confirm sign-in first");
        }
    }

    public void confirmed(Long userId, HttpSession session) {
        session.setAttribute(RECENT, new Recent(userId, Instant.now().plusSeconds(300)));
    }
}
