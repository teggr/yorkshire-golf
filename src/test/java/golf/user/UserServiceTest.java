package golf.user;

import golf.email.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @Test
    void registerTranslatesDuplicateEmailConstraintIntoDomainException() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        PasswordResetTokenRepository tokenRepository = mock(PasswordResetTokenRepository.class);
        EmailService emailService = mock(EmailService.class);
        UserService service = new UserService(userRepository, passwordEncoder, tokenRepository, emailService);

        when(userRepository.existsByEmail("user@example.com")).thenReturn(false, true);
        when(passwordEncoder.encode("GolfClub99")).thenReturn("encoded-password");
        when(userRepository.findByTrackerId(any())).thenReturn(Optional.empty());
        when(userRepository.save(any(GolfUser.class))).thenThrow(new DuplicateKeyException("uk_golf_user_email"));

        assertThatThrownBy(() -> service.register(" User@Example.com ", "GolfClub99"))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("user@example.com");

        verify(userRepository, times(2)).existsByEmail("user@example.com");
    }
}