package golf.user;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordResetTokenRepositoryTest {

    private DataSource dataSource;
    private JdbcTemplate jdbcTemplate;
    private PasswordResetTokenRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new DriverManagerDataSource("jdbc:h2:mem:tokenrepo;MODE=LEGACY;DB_CLOSE_DELAY=-1", "sa", "");
        initializeSchemaWithLiquibase(dataSource);

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

    private void initializeSchemaWithLiquibase(DataSource source) throws LiquibaseException {
        try (Connection connection = source.getConnection()) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(
                    "db/changelog/db.changelog-master.yaml",
                    new ClassLoaderResourceAccessor(),
                    database
            );
            liquibase.update(new Contexts(), new LabelExpression());
        } catch (Exception ex) {
            if (ex instanceof LiquibaseException liquibaseException) {
                throw liquibaseException;
            }
            throw new LiquibaseException("Failed to initialize Liquibase schema for test", ex);
        }
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
