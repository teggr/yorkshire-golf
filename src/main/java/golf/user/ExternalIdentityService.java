package golf.user;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ExternalIdentityService {
    public record Identity(String provider, String subject, Long userId) {}

    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final TransactionTemplate transactions;

    public ExternalIdentityService(JdbcTemplate jdbc, UserRepository users, PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        this.users = users;
        this.transactions = new TransactionTemplate(manager);
    }

    public Optional<Identity> find(String provider, String subject) {
        return jdbc.query("SELECT provider, subject, user_id FROM external_identity WHERE provider = ? AND subject = ?",
                (rs, n) -> new Identity(rs.getString(1), rs.getString(2), rs.getLong(3)),
                provider, subject).stream().findFirst();
    }

    public List<Identity> list(Long userId) {
        return jdbc.query("SELECT provider, subject, user_id FROM external_identity WHERE user_id = ? ORDER BY provider",
                (rs, n) -> new Identity(rs.getString(1), rs.getString(2), rs.getLong(3)), userId);
    }

    public GolfUser login(String provider, String subject) {
        validate(provider, subject);
        try {
            return transactions.execute(status -> {
                Optional<Identity> identity = find(provider, subject);
                if (identity.isPresent()) return usable(identity.get().userId());
                GolfUser user = users.save(new GolfUser(null, null, null, UUID.randomUUID().toString(),
                        "USER", false, 0));
                jdbc.update("INSERT INTO external_identity(provider, subject, user_id) VALUES (?, ?, ?)",
                        provider, subject, user.id());
                return user;
            });
        } catch (DataIntegrityViolationException race) {
            // The failed creation transaction rolls back its user before resolving the winner.
            return find(provider, subject).map(identity -> usable(identity.userId())).orElseThrow(() -> race);
        }
    }

    public GolfUser link(Long userId, String provider, String subject) {
        validate(provider, subject);
        return transactions.execute(status -> {
            lock(userId);
            GolfUser user = usable(userId);
            if (find(provider, subject).isPresent()
                    || list(userId).stream().anyMatch(identity -> identity.provider().equals(provider))) {
                throw new IllegalArgumentException("Identity already linked");
            }
            jdbc.update("INSERT INTO external_identity(provider, subject, user_id) VALUES (?, ?, ?)",
                    provider, subject, userId);
            return user;
        });
    }

    public void unlink(Long userId, String provider, List<String> enabledProviders) {
        transactions.executeWithoutResult(status -> {
            lock(userId);
            GolfUser user = usable(userId);
            List<Identity> identities = list(userId);
            boolean password = user.email() != null && user.password() != null;
            long remaining = identities.stream().filter(i -> !i.provider().equals(provider))
                    .filter(i -> enabledProviders.contains(i.provider())).count();
            if (!password && remaining == 0) throw new IllegalArgumentException("Cannot remove last sign-in method");
            jdbc.update("DELETE FROM external_identity WHERE user_id = ? AND provider = ?", userId, provider);
        });
    }

    private void lock(Long userId) {
        jdbc.queryForObject("SELECT id FROM golf_user WHERE id = ? FOR UPDATE", Long.class, userId);
    }

    public GolfUser usable(Long userId) {
        GolfUser user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("Unknown account"));
        if (user.accountLocked()) throw new LockedException("Account unavailable");
        return user;
    }

    private void validate(String provider, String subject) {
        if (!List.of("google", "apple", "facebook", "x").contains(provider)
                || subject == null || subject.isBlank() || subject.length() > 255) {
            throw new IllegalArgumentException("Invalid identity");
        }
    }
}
