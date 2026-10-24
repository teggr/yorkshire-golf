package golf.security;

import golf.user.ExternalIdentityService;
import golf.user.GolfUser;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountActionGuardTest {
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();
    private final ExternalIdentityService identities = mock(ExternalIdentityService.class);
    private final AccountActionGuard guard = new AccountActionGuard(passwords, identities);

    @Test
    void passwordAccountsMustSupplyCurrentPasswordEvenAfterProviderReauth() {
        GolfUser local = new GolfUser(1L, "local@example.com", passwords.encode("correct"),
                "tracker", "USER", false, 0);
        var session = new MockHttpSession();
        guard.confirmed(local.id(), session);
        assertThatThrownBy(() -> guard.requireConfirmation(local, null, session)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> guard.requireConfirmation(local, "wrong", session)).isInstanceOf(IllegalArgumentException.class);
        guard.requireConfirmation(local, "correct", session);
    }

    @Test
    void externalConfirmationIsSingleUseAndBoundToAccount() {
        GolfUser first = new GolfUser(1L, null, null, "tracker", "USER", false, 0);
        GolfUser second = new GolfUser(2L, null, null, "other", "USER", false, 0);
        var session = new MockHttpSession();
        assertThatThrownBy(() -> guard.requireConfirmation(first, null, session)).isInstanceOf(IllegalArgumentException.class);
        guard.confirmed(first.id(), session);
        assertThatThrownBy(() -> guard.requireConfirmation(second, null, session)).isInstanceOf(IllegalArgumentException.class);
        guard.confirmed(first.id(), session);
        guard.requireConfirmation(first, null, session);
        assertThatThrownBy(() -> guard.requireConfirmation(first, null, session)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rateLimitsRepeatedSensitiveOperationsPerLocalAccount() {
        for (int i = 0; i < 10; i++) guard.rateLimit(1L);
        assertThatThrownBy(() -> guard.rateLimit(1L)).isInstanceOf(IllegalArgumentException.class);
        guard.rateLimit(2L);
    }
}
