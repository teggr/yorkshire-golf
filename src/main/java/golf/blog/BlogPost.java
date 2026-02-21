package golf.blog;

import java.util.List;

public record BlogPost(
        String id,
        String title,
        String date,
        String courseName,
        List<String> imageUrls,
        String content) {
}
