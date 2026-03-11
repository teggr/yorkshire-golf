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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserRepositoryTest {

    private JdbcTemplate jdbcTemplate;
    private UserRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        DataSource dataSource = new DriverManagerDataSource("jdbc:h2:mem:userrepo;MODE=LEGACY;DB_CLOSE_DELAY=-1", "sa", "");
        initializeSchemaWithLiquibase(dataSource);

        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new UserRepository(jdbcTemplate);
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
    void saveRejectsDuplicateEmailAddresses() {
        repository.save(new GolfUser(null, "user@example.com", "hash", "tracker-1", "USER", false));

        assertThatThrownBy(() -> repository.save(new GolfUser(null, "user@example.com", "hash", "tracker-2", "USER", false)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}