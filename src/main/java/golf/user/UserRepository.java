package golf.user;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<GolfUser> USER_ROW_MAPPER = (rs, rowNum) -> new GolfUser(
            rs.getLong("id"),
            rs.getString("email"),
            rs.getString("password"),
            rs.getString("security_question"),
            rs.getString("security_answer"),
            rs.getString("tracker_id"),
            rs.getString("role"),
            rs.getBoolean("account_locked"),
            rs.getInt("failed_security_attempts")
    );

    public Optional<GolfUser> findByEmail(String email) {
        List<GolfUser> users = jdbc.query(
                "SELECT * FROM golf_user WHERE email = ?",
                USER_ROW_MAPPER,
                email
        );
        return users.stream().findFirst();
    }

    public Optional<GolfUser> findByTrackerId(String trackerId) {
        List<GolfUser> users = jdbc.query(
                "SELECT * FROM golf_user WHERE tracker_id = ?",
                USER_ROW_MAPPER,
                trackerId
        );
        return users.stream().findFirst();
    }

    public Optional<GolfUser> findById(Long id) {
        List<GolfUser> users = jdbc.query(
                "SELECT * FROM golf_user WHERE id = ?",
                USER_ROW_MAPPER,
                id
        );
        return users.stream().findFirst();
    }

    public List<GolfUser> findAll() {
        return jdbc.query("SELECT * FROM golf_user ORDER BY email", USER_ROW_MAPPER);
    }

    public GolfUser save(GolfUser user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO golf_user (email, password, security_question, security_answer, tracker_id, role, account_locked, failed_security_attempts) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, user.email());
            ps.setString(2, user.password());
            ps.setString(3, user.securityQuestion());
            ps.setString(4, user.securityAnswer());
            ps.setString(5, user.trackerId());
            ps.setString(6, user.role());
            ps.setBoolean(7, user.accountLocked());
            ps.setInt(8, user.failedSecurityAttempts());
            return ps;
        }, keyHolder);
        long id = keyHolder.getKey().longValue();
        return new GolfUser(id, user.email(), user.password(), user.securityQuestion(),
                user.securityAnswer(), user.trackerId(), user.role(), user.accountLocked(),
                user.failedSecurityAttempts());
    }

    public void updateFailedSecurityAttempts(Long userId, int attempts) {
        jdbc.update(
                "UPDATE golf_user SET failed_security_attempts = ? WHERE id = ?",
                attempts, userId
        );
    }

    public void updateAccountLocked(Long userId, boolean locked) {
        jdbc.update(
                "UPDATE golf_user SET account_locked = ? WHERE id = ?",
                locked, userId
        );
    }

    public void resetFailedSecurityAttempts(Long userId) {
        jdbc.update(
                "UPDATE golf_user SET failed_security_attempts = 0, account_locked = FALSE WHERE id = ?",
                userId
        );
    }

    public void updatePassword(Long userId, String encodedPassword) {
        jdbc.update(
                "UPDATE golf_user SET password = ? WHERE id = ?",
                encodedPassword, userId
        );
    }

    public boolean existsByEmail(String email) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM golf_user WHERE email = ?",
                Integer.class,
                email
        );
        return count != null && count > 0;
    }

}
