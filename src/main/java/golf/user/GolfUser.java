package golf.user;

public record GolfUser(
        Long id,
        String email,
        String password,
        String trackerId,
        String role,
        boolean accountLocked,
        int failedLoginAttempts
) {
}
