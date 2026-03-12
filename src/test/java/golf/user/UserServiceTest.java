package golf.user;

import golf.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private PasswordResetTokenRepository tokenRepository;
    private EmailService emailService;
    private UserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        tokenRepository = mock(PasswordResetTokenRepository.class);
        emailService = mock(EmailService.class);
        service = new UserService(userRepository, passwordEncoder, tokenRepository, emailService);
    }

    @Test
    void registerTranslatesDuplicateEmailConstraintIntoDomainException() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false, true);
        when(passwordEncoder.encode("GolfClub99")).thenReturn("encoded-password");
        when(userRepository.findByTrackerId(any())).thenReturn(Optional.empty());
        when(userRepository.save(any(GolfUser.class))).thenThrow(new DuplicateKeyException("uk_golf_user_email"));

        assertThatThrownBy(() -> service.register(" User@Example.com ", "GolfClub99"))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("user@example.com");

        verify(userRepository, times(2)).existsByEmail("user@example.com");
    }

    @Test
    void resetPasswordReturnsInvalidWhenTokenMissing() {
        when(tokenRepository.findByToken("missing-token")).thenReturn(Optional.empty());

        UserService.PasswordResetResult result = service.resetPassword("missing-token", "GolfClub99");

        assertEquals(UserService.PasswordResetResult.INVALID_TOKEN, result);
        verify(userRepository, never()).updatePassword(anyLong(), anyString());
    }

    @Test
    void resetPasswordReturnsInvalidWhenTokenAlreadyUsed() {
        PasswordResetToken token = new PasswordResetToken(
                1L,
                99L,
                "hash",
                Instant.now(),
                true
        );
        when(tokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        UserService.PasswordResetResult result = service.resetPassword("used-token", "GolfClub99");

        assertEquals(UserService.PasswordResetResult.INVALID_TOKEN, result);
        verify(tokenRepository, never()).markUsedIfUnused(anyLong());
        verify(userRepository, never()).updatePassword(anyLong(), anyString());
    }

    @Test
    void resetPasswordReturnsExpiredWhenTokenTooOld() {
        PasswordResetToken token = new PasswordResetToken(
                2L,
                99L,
                "hash",
                Instant.now().minusSeconds(7201),
                false
        );
        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        UserService.PasswordResetResult result = service.resetPassword("expired-token", "GolfClub99");

        assertEquals(UserService.PasswordResetResult.EXPIRED_TOKEN, result);
        verify(tokenRepository, never()).markUsedIfUnused(anyLong());
        verify(userRepository, never()).updatePassword(anyLong(), anyString());
    }

    @Test
    void resetPasswordReturnsInvalidWhenTokenCannotBeConsumed() {
        PasswordResetToken token = new PasswordResetToken(
                3L,
                77L,
                "hash",
                Instant.now(),
                false
        );
        when(tokenRepository.findByToken("race-token")).thenReturn(Optional.of(token));
        when(tokenRepository.markUsedIfUnused(3L)).thenReturn(false);

        UserService.PasswordResetResult result = service.resetPassword("race-token", "GolfClub99");

        assertEquals(UserService.PasswordResetResult.INVALID_TOKEN, result);
        verify(userRepository, never()).updatePassword(anyLong(), anyString());
    }

    @Test
    void resetPasswordReturnsSuccessAndUpdatesPasswordAndLockState() {
        PasswordResetToken token = new PasswordResetToken(
                4L,
                55L,
                "hash",
                Instant.now(),
                false
        );
        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(tokenRepository.markUsedIfUnused(4L)).thenReturn(true);
        when(passwordEncoder.encode("GolfClub99")).thenReturn("encoded-99");

        UserService.PasswordResetResult result = service.resetPassword("valid-token", "GolfClub99");

        assertEquals(UserService.PasswordResetResult.SUCCESS, result);
        verify(userRepository).updatePassword(55L, "encoded-99");
        verify(userRepository).updateAccountLocked(55L, false);
        verify(userRepository).resetFailedLoginAttempts(55L);
    }
}