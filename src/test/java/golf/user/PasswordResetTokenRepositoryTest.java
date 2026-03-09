package golf.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordResetTokenRepositoryTest {

    private DataSource dataSource;
    private JdbcTemplate jdbcTemplate;
    private PasswordResetTokenRepository repository;

    @BeforeEach
    void setUp() {
        dataSource = new DriverManagerDataSource("jdbc:h2:mem:tokenrepo;MODE=LEGACY;DB_CLOSE_DELAY=-1", "sa", "");
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource("schema.sql"));
        populator.execute(dataSource);

        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new PasswordResetTokenRepository(jdbcTemplate);

        jdbcTemplate.update(
                "INSERT INTO golf_user (email, password, tracker_id, role, account_locked) VALUES (?, ?, ?, ?, ?)",
                "test@example.com", "hash", "tracker123", "USER", false
        );
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DROP ALL OBJECTS");
    }

    @Test
    void savesAndFindsByRawTokenUsingHashLookup() {
        repository.save(1L, "plain-token-123");

        Optional<PasswordResetToken> found = repository.findByToken("plain-token-123");

        assertThat(found).isPresent();
        assertThat(found.get().tokenHash()).isNotBlank();
    }

    @Test
    void consumesTokenOnlyOnce() {
        PasswordResetToken token = repository.save(1L, "plain-token-123");

        assertThat(repository.markUsedIfUnused(token.id())).isTrue();
        assertThat(repository.markUsedIfUnused(token.id())).isFalse();
    }
}
