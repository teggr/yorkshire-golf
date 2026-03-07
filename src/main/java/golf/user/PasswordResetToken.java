package golf.user;

import java.time.Instant;

public record PasswordResetToken(
        Long id,
        Long userId,
        String token,
        Instant createdAt,
        boolean used
) {
}
