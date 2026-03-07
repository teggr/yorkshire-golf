package golf.user;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserRoundRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<UserRound> ROUND_ROW_MAPPER = (rs, rowNum) -> new UserRound(
            String.valueOf(rs.getLong("id")),
            rs.getLong("user_id"),
            null,
            rs.getDate("date").toLocalDate().toString(),
            rs.getString("course_name"),
            null,
            null,
            null
    );

    public List<UserRound> findByUserId(Long userId) {
        return jdbc.query(
                "SELECT * FROM user_round WHERE user_id = ? ORDER BY date DESC",
                ROUND_ROW_MAPPER,
                userId
        );
    }

    public List<UserRound> findByCourseName(String courseName) {
        return jdbc.query(
                "SELECT * FROM user_round WHERE course_name = ?",
                ROUND_ROW_MAPPER,
                courseName
        );
    }

    public boolean existsByUserIdAndCourseName(Long userId, String courseName) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_round WHERE user_id = ? AND course_name = ?",
                Integer.class,
                userId, courseName
        );
        return count != null && count > 0;
    }

    public UserRound save(Long userId, String courseName, String date) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO user_round (user_id, course_name, date) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setLong(1, userId);
            ps.setString(2, courseName);
            ps.setDate(3, Date.valueOf(date));
            return ps;
        }, keyHolder);
        long id = keyHolder.getKey().longValue();
        return new UserRound(String.valueOf(id), userId, null, date, courseName, null, null, null);
    }

    public void updateDate(Long roundId, Long userId, String date) {
        jdbc.update(
                "UPDATE user_round SET date = ? WHERE id = ? AND user_id = ?",
                Date.valueOf(date), roundId, userId
        );
    }

    public void deleteByIdAndUserId(Long roundId, Long userId) {
        jdbc.update(
                "DELETE FROM user_round WHERE id = ? AND user_id = ?",
                roundId, userId
        );
    }

}
