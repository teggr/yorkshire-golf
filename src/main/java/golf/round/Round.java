package golf.round;

import java.util.List;

public record Round(
        String id,
        String title,
        String date,
        String courseName,
        List<String> imageUrls,
        String content) {
}
