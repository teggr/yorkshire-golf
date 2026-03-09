package golf.challenge;

import java.util.List;

public record MonthlyCourseProgress(
        List<String> labels,
        List<Long> cumulativeCoursesPlayed
) {
}
