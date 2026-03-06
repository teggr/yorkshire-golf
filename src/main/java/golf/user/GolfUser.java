package golf.user;

public record GolfUser(
        Long id,
        String email,
        String password,
        String securityQuestion,
        String securityAnswer,
        String trackerId,
        String role,
        boolean accountLocked,
        int failedSecurityAttempts
) {
}
