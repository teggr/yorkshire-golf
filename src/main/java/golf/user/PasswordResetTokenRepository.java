package golf.user;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PasswordResetTokenRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<PasswordResetToken> TOKEN_ROW_MAPPER = (rs, rowNum) -> new PasswordResetToken(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("token_hash"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getBoolean("used")
    );

    @PostConstruct
    void initializeSchemaUpgrades() {
        jdbc.execute("ALTER TABLE password_reset_token ADD COLUMN IF NOT EXISTS token_hash VARCHAR(255)");
        jdbc.execute("ALTER TABLE password_reset_token ALTER COLUMN token DROP NOT NULL");
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_password_reset_token_hash ON password_reset_token(token_hash)");

        // Invalidate any legacy plaintext tokens during rollout.
        jdbc.update("UPDATE password_reset_token SET used = TRUE WHERE token_hash IS NULL AND used = FALSE");
    }

    public PasswordResetToken save(Long userId, String token) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        Instant now = Instant.now();
        String tokenHash = hashToken(token);
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO password_reset_token (user_id, token, token_hash, created_at, used) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, userId);
            ps.setString(2, null);
            ps.setString(3, tokenHash);
            ps.setTimestamp(4, Timestamp.from(now));
            ps.setBoolean(5, false);
            return ps;
        }, keyHolder);
        long id = keyHolder.getKey().longValue();
        return new PasswordResetToken(id, userId, tokenHash, now, false);
    }

    public Optional<PasswordResetToken> findByToken(String token) {
        String tokenHash = hashToken(token);
        List<PasswordResetToken> results = jdbc.query(
                "SELECT * FROM password_reset_token WHERE token_hash = ?",
                TOKEN_ROW_MAPPER,
                tokenHash
        );
        return results.stream().findFirst();
    }

    public void markUsed(Long id) {
        jdbc.update("UPDATE password_reset_token SET used = TRUE WHERE id = ?", id);
    }

    public boolean markUsedIfUnused(Long id) {
        int updated = jdbc.update("UPDATE password_reset_token SET used = TRUE WHERE id = ? AND used = FALSE", id);
        return updated == 1;
    }

    public void invalidateAllForUser(Long userId) {
        jdbc.update("UPDATE password_reset_token SET used = TRUE WHERE user_id = ?", userId);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

}
