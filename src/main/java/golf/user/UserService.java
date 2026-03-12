package golf.user;

import golf.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String TRACKER_ID_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int TRACKER_ID_LENGTH = 12;
    private static final long RESET_TOKEN_EXPIRY_HOURS = 1;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    public GolfUser register(String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }

        String encodedPassword = passwordEncoder.encode(rawPassword);
        String trackerId = generateTrackerId();
        GolfUser user = new GolfUser(null, normalizedEmail, encodedPassword,
                trackerId, "USER", false, 0);
        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            if (userRepository.existsByEmail(normalizedEmail)) {
                throw new DuplicateEmailException(normalizedEmail, ex);
            }
            throw ex;
        }
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(normalizeEmail(email));
    }

    public Optional<GolfUser> findByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email));
    }

    public Optional<GolfUser> findByTrackerId(String trackerId) {
        return userRepository.findByTrackerId(trackerId);
    }

    public Optional<GolfUser> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Generates a password reset token for the given email address and sends a reset link.
     * Does nothing (silently) when the email is not registered, to avoid user enumeration.
     */
    public void sendPasswordResetEmail(String email, String resetBaseUrl) {
        Optional<GolfUser> userOpt = findByEmail(email);
        if (userOpt.isEmpty()) {
            return;
        }
        GolfUser user = userOpt.get();
        String token = generateSecureToken();
        passwordResetTokenRepository.save(user.id(), token);
        String resetLink = resetBaseUrl + "?token=" + token;
        String body = "Hello,\n\n"
                + "You requested a password reset for your Yorkshire Golf Life account.\n\n"
                + "Click the link below to reset your password (valid for " + RESET_TOKEN_EXPIRY_HOURS + " hour):\n\n"
                + resetLink + "\n\n"
                + "If you did not request this, you can safely ignore this email.\n\n"
                + "Yorkshire Golf Life";
        emailService.sendEmail(email, "Reset your Yorkshire Golf Life password", body);
    }

    /**
     * Validates the reset token and resets the password if the token is valid and unexpired.
     */
    public PasswordResetResult resetPassword(String token, String newRawPassword) {
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            return PasswordResetResult.INVALID_TOKEN;
        }
        PasswordResetToken resetToken = tokenOpt.get();
        if (resetToken.used()) {
            return PasswordResetResult.INVALID_TOKEN;
        }
        Instant expiry = resetToken.createdAt().plus(RESET_TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS);
        if (Instant.now().isAfter(expiry)) {
            return PasswordResetResult.EXPIRED_TOKEN;
        }
        if (!passwordResetTokenRepository.markUsedIfUnused(resetToken.id())) {
            return PasswordResetResult.INVALID_TOKEN;
        }
        String encodedPassword = passwordEncoder.encode(newRawPassword);
        userRepository.updatePassword(resetToken.userId(), encodedPassword);
        userRepository.updateAccountLocked(resetToken.userId(), false);
        userRepository.resetFailedLoginAttempts(resetToken.userId());
        return PasswordResetResult.SUCCESS;
    }

    public void unlockAccount(Long userId) {
        userRepository.updateAccountLocked(userId, false);
        userRepository.resetFailedLoginAttempts(userId);
        passwordResetTokenRepository.invalidateAllForUser(userId);
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

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String normalizeEmail(String email) {
        return email.toLowerCase().trim();
    }

    public enum PasswordResetResult {
        SUCCESS,
        INVALID_TOKEN,
        EXPIRED_TOKEN
    }

}
