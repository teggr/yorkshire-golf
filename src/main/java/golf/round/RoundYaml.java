package golf.round;

import java.util.List;

/**
 * YAML deserialization target for round data files in src/main/resources/rounds/.
 * Used internally by {@link Rounds} to load round data and convert to {@link golf.user.UserRound}.
 */
public record RoundYaml(
        String id,
        String title,
        String date,
        String courseName,
        List<String> imageUrls,
        String content
) {
}
