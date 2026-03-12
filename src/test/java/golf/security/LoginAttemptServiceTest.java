package golf.security;

import golf.user.GolfUser;
import golf.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LoginAttemptServiceTest {

    private UserRepository userRepository;
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new LoginAttemptService(userRepository);
    }

    @Test
    void recordFailureIncrementsAttemptsForKnownUser() {
        GolfUser user = unlockedUser(1L, "test@example.com", 3);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        service.recordFailure("test@example.com");

        verify(userRepository).incrementFailedLoginAttempts(1L, LoginAttemptService.MAX_FAILURES);
    }

    @Test
    void recordFailureDoesNothingForUnknownEmail() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        service.recordFailure("unknown@example.com");

        verify(userRepository, never()).incrementFailedLoginAttempts(anyLong(), anyInt());
    }

    @Test
    void recordSuccessResetsAttemptsForKnownUser() {
        GolfUser user = unlockedUser(2L, "test@example.com", 4);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        service.recordSuccess("test@example.com");

        verify(userRepository).resetFailedLoginAttempts(2L);
    }

    @Test
    void isLockedReturnsTrueWhenAccountLocked() {
        GolfUser locked = lockedUser(3L, "locked@example.com");
        when(userRepository.findByEmail("locked@example.com")).thenReturn(Optional.of(locked));

        assertThat(service.isLocked("locked@example.com")).isTrue();
    }

    @Test
    void isLockedReturnsFalseWhenAccountNotLocked() {
        GolfUser active = unlockedUser(4L, "active@example.com", 0);
        when(userRepository.findByEmail("active@example.com")).thenReturn(Optional.of(active));

        assertThat(service.isLocked("active@example.com")).isFalse();
    }

    @Test
    void isLockedReturnsFalseForUnknownEmail() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThat(service.isLocked("nobody@example.com")).isFalse();
    }

    @Test
    void normalizesEmailBeforeLookup() {
        GolfUser user = unlockedUser(5L, "test@example.com", 0);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        service.recordFailure("  TEST@EXAMPLE.COM  ");

        verify(userRepository).incrementFailedLoginAttempts(5L, LoginAttemptService.MAX_FAILURES);
    }

    @Test
    void maxFailuresThresholdIsTen() {
        assertThat(LoginAttemptService.MAX_FAILURES).isEqualTo(10);
    }

    private static GolfUser unlockedUser(Long id, String email, int failedAttempts) {
        return new GolfUser(id, email, "password", "tracker", "USER", false, failedAttempts);
    }

    private static GolfUser lockedUser(Long id, String email) {
        return new GolfUser(id, email, "password", "tracker", "USER", true, 10);
    }
}
