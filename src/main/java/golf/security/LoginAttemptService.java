package golf.security;

import golf.user.GolfUser;
import golf.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    static final int MAX_FAILURES = 10;

    private final UserRepository userRepository;

    public void recordFailure(String email) {
        String key = normalize(email);
        if (key == null) {
            return;
        }
        userRepository.findByEmail(key).ifPresent(user ->
                userRepository.incrementFailedLoginAttempts(user.id(), MAX_FAILURES));
    }

    public void recordSuccess(String email) {
        String key = normalize(email);
        if (key == null) {
            return;
        }
        userRepository.findByEmail(key).ifPresent(user ->
                userRepository.resetFailedLoginAttempts(user.id()));
    }

    public boolean isLocked(String email) {
        String key = normalize(email);
        if (key == null) {
            return false;
        }
        return userRepository.findByEmail(key)
                .map(GolfUser::accountLocked)
                .orElse(false);
    }

    private String normalize(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }
}
