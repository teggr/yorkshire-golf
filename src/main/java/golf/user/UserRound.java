package golf.user;

import golf.course.Course;

import java.util.List;

public record UserRound(
        String id,
        Long userId,
        String title,
        String date,
        String courseName,
        Course course,
        List<String> imageUrls,
        String content
) {
}
