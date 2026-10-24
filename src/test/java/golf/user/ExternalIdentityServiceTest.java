package golf.user;

import golf.GolfTrackerApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.LockedException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = GolfTrackerApplication.class,
        properties = {"spring.datasource.url=jdbc:h2:mem:external-identities;DB_CLOSE_DELAY=-1", "spring.mail.host=localhost"})
class ExternalIdentityServiceTest {
    @Autowired ExternalIdentityService identities;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRoundRepository rounds;

    @Test
    void createsAnonymousLocalAccountAndMapsRepeatedLoginWithoutEmailLinking() {
        GolfUser existing = users.save(new GolfUser(null, "existing-" + UUID.randomUUID() + "@example.com",
                "hash", UUID.randomUUID().toString(), "USER", false, 0));
        String subject = UUID.randomUUID().toString();
        GolfUser external = identities.login("google", subject);
        assertThat(external.id()).isNotEqualTo(existing.id());
        assertThat(external.email()).isNull();
        assertThat(external.password()).isNull();
        assertThat(external.role()).isEqualTo("USER");
        assertThat(identities.login("google", subject).id()).isEqualTo(external.id());
        assertThat(identities.login("apple", subject).id()).isNotEqualTo(external.id());
    }

    @Test
    void concurrentFirstLoginsCreateExactlyOneAccountAndIdentity() throws Exception {
        String subject = UUID.randomUUID().toString();
        int before = jdbc.queryForObject("SELECT COUNT(*) FROM golf_user", Integer.class);
        var executor = Executors.newFixedThreadPool(6);
        try {
            List<Callable<Long>> logins = new ArrayList<>();
            for (int i = 0; i < 6; i++) logins.add(() -> identities.login("google", subject).id());
            var results = executor.invokeAll(logins);
            var ids = new ArrayList<Long>();
            for (var result : results) ids.add(result.get());
            assertThat(ids.stream().distinct()).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM golf_user", Integer.class)).isEqualTo(before + 1);
    }

    @Test
    void rejectsOwnedIdentityAndNeverRemovesLastUsableMethod() {
        String first = UUID.randomUUID().toString();
        GolfUser owner = identities.login("google", first);
        GolfUser other = identities.login("apple", UUID.randomUUID().toString());
        assertThatThrownBy(() -> identities.link(other.id(), "google", first)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> identities.unlink(owner.id(), "google", List.of("google", "apple")))
                .isInstanceOf(IllegalArgumentException.class);
        identities.link(owner.id(), "apple", UUID.randomUUID().toString());
        assertThatThrownBy(() -> identities.unlink(owner.id(), "google", List.of("google")))
                .isInstanceOf(IllegalArgumentException.class);
        identities.unlink(owner.id(), "google", List.of("google", "apple"));
        assertThat(identities.list(owner.id())).extracting(ExternalIdentityService.Identity::provider).containsExactly("apple");
    }

    @Test
    void existingPasswordAccountCanUnlinkAndRetainsTrackerAndRounds() {
        GolfUser user = users.save(new GolfUser(null, UUID.randomUUID() + "@example.com", "hash",
                UUID.randomUUID().toString(), "ADMIN", false, 0));
        var round = rounds.save(user.id(), "Alwoodley", "2026-10-24");
        identities.link(user.id(), "facebook", UUID.randomUUID().toString());
        identities.unlink(user.id(), "facebook", List.of("facebook"));
        assertThat(users.findById(user.id())).contains(user);
        assertThat(identities.list(user.id())).isEmpty();
        assertThat(rounds.findByUserId(user.id())).containsExactly(round);
    }

    @Test
    void lockedAccountsCannotSignInOrChangeIdentities() {
        String subject = UUID.randomUUID().toString();
        GolfUser user = identities.login("facebook", subject);
        users.updateAccountLocked(user.id(), true);
        assertThatThrownBy(() -> identities.login("facebook", subject)).isInstanceOf(LockedException.class);
        assertThatThrownBy(() -> identities.link(user.id(), "apple", subject)).isInstanceOf(LockedException.class);
    }
}
