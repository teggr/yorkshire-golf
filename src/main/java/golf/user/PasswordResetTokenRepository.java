package golf.user;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PasswordResetTokenRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<PasswordResetToken> TOKEN_ROW_MAPPER = (rs, rowNum) -> new PasswordResetToken(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("token"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getBoolean("used")
    );

    public PasswordResetToken save(Long userId, String token) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        Instant now = Instant.now();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO password_reset_token (user_id, token, created_at, used) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, userId);
            ps.setString(2, token);
            ps.setTimestamp(3, Timestamp.from(now));
            ps.setBoolean(4, false);
            return ps;
        }, keyHolder);
        long id = keyHolder.getKey().longValue();
        return new PasswordResetToken(id, userId, token, now, false);
    }

    public Optional<PasswordResetToken> findByToken(String token) {
        List<PasswordResetToken> results = jdbc.query(
                "SELECT * FROM password_reset_token WHERE token = ?",
                TOKEN_ROW_MAPPER,
                token
        );
        return results.stream().findFirst();
    }

    public void markUsed(Long id) {
        jdbc.update("UPDATE password_reset_token SET used = TRUE WHERE id = ?", id);
    }

    public void invalidateAllForUser(Long userId) {
        jdbc.update("UPDATE password_reset_token SET used = TRUE WHERE user_id = ?", userId);
    }

}
