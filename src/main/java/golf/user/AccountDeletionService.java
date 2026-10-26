package golf.user;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private final JdbcTemplate jdbc;

    @Transactional
    public void delete(Long userId) {
        jdbc.queryForObject("SELECT id FROM golf_user WHERE id = ? FOR UPDATE", Long.class, userId);
        jdbc.update("DELETE FROM password_reset_token WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM user_round WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM external_identity WHERE user_id = ?", userId);
        if (jdbc.update("DELETE FROM golf_user WHERE id = ?", userId) != 1) {
            throw new IllegalArgumentException("Unknown account");
        }
    }
}
