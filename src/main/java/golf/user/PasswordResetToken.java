package golf.user;

import java.time.Instant;

public record PasswordResetToken(
        Long id,
        Long userId,
        String tokenHash,
        Instant createdAt,
        boolean used
) {
}
