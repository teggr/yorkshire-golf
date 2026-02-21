package golf.blog;

public record BlogPost(
        String id,
        String title,
        String date,
        String courseName,
        String imageUrl,
        String content) {
}
