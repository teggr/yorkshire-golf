package golf.round;

import golf.course.Course;

import java.util.List;

public record Round(
        String id,
        String title,
        String date,
        String courseName,
        Course course,
        List<String> imageUrls,
        String content) {
}
