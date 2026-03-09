package golf.user;

import java.util.Optional;

public class PasswordPolicy {

    private static final int MIN_LENGTH = 8;
    private static final String ALPHANUMERIC_PATTERN = "[A-Za-z0-9]+";

    private PasswordPolicy() {}

    /**
     * Validates a password against the site policy.
     *
     * @return an empty Optional if the password is valid, or an Optional containing an error message.
     */
    public static Optional<String> validate(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return Optional.of("Password must be at least " + MIN_LENGTH + " characters.");
        }
        if (!password.matches(ALPHANUMERIC_PATTERN)) {
            return Optional.of("Password must contain only letters and numbers (no spaces or symbols).");
        }
        return Optional.empty();
    }
}
