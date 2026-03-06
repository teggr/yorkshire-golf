package golf.user;

public record UserRound(
        Long id,
        Long userId,
        String courseName,
        String date
) {
}
