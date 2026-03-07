package golf.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int MAX_SECURITY_ATTEMPTS = 3;
    private static final String TRACKER_ID_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int TRACKER_ID_LENGTH = 12;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public GolfUser register(String email, String rawPassword, String securityQuestion, String rawSecurityAnswer) {
        String encodedPassword = passwordEncoder.encode(rawPassword);
        String encodedAnswer = passwordEncoder.encode(rawSecurityAnswer.toLowerCase().trim());
        String trackerId = generateTrackerId();
        GolfUser user = new GolfUser(null, email.toLowerCase().trim(), encodedPassword,
                securityQuestion, encodedAnswer, trackerId, "USER", false, 0);
        return userRepository.save(user);
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email.toLowerCase().trim());
    }

    public Optional<GolfUser> findByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim());
    }

    public Optional<GolfUser> findByTrackerId(String trackerId) {
        return userRepository.findByTrackerId(trackerId);
    }

    public Optional<GolfUser> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Attempt to answer the security question.
     * Returns true if the answer is correct and the password has been reset.
     * Returns false if the answer is incorrect.
     * Throws AccountLockedException if the account is now locked.
     */
    public SecurityAnswerResult attemptSecurityAnswer(GolfUser user, String rawAnswer, String newRawPassword) {
        if (user.accountLocked()) {
            return SecurityAnswerResult.ACCOUNT_LOCKED;
        }

        boolean correct = passwordEncoder.matches(rawAnswer.toLowerCase().trim(), user.securityAnswer());
        if (correct) {
            String encodedPassword = passwordEncoder.encode(newRawPassword);
            userRepository.updatePassword(user.id(), encodedPassword);
            userRepository.resetFailedSecurityAttempts(user.id());
            return SecurityAnswerResult.SUCCESS;
        }

        int newAttempts = user.failedSecurityAttempts() + 1;
        if (newAttempts >= MAX_SECURITY_ATTEMPTS) {
            userRepository.updateFailedSecurityAttempts(user.id(), newAttempts);
            userRepository.updateAccountLocked(user.id(), true);
            return SecurityAnswerResult.ACCOUNT_LOCKED;
        }

        userRepository.updateFailedSecurityAttempts(user.id(), newAttempts);
        return SecurityAnswerResult.WRONG_ANSWER;
    }

    public int remainingSecurityAttempts(GolfUser user) {
        return MAX_SECURITY_ATTEMPTS - user.failedSecurityAttempts();
    }

    public void unlockAccount(Long userId) {
        userRepository.resetFailedSecurityAttempts(userId);
    }

    private String generateTrackerId() {
        SecureRandom random = new SecureRandom();
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(TRACKER_ID_LENGTH);
            for (int i = 0; i < TRACKER_ID_LENGTH; i++) {
                sb.append(TRACKER_ID_CHARS.charAt(random.nextInt(TRACKER_ID_CHARS.length())));
            }
            String candidate = sb.toString();
            if (userRepository.findByTrackerId(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate a unique tracker ID after 10 attempts");
    }

    public enum SecurityAnswerResult {
        SUCCESS,
        WRONG_ANSWER,
        ACCOUNT_LOCKED
    }

}
