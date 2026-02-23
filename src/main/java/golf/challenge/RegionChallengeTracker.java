package golf.challenge;

import golf.course.Region;
import golf.round.Round;

import java.util.List;
import java.util.Map;

public record RegionChallengeTracker(
        Map<Region, Long> courseCountByRegion,
        List<Round> rounds
) {

    public long totalCourseCount() {
        return courseCountByRegion.values().stream()
                .reduce(0L, Long::sum);
    }

    public long totalCoursesPlayed() {
        return rounds().size();
    }

    public long totalCoursesToBePlayed() {
        return totalCourseCount() - totalCoursesPlayed();
    }

    public long totalCourseCount(Region region) {
        return courseCountByRegion.get(region);
    }

    public long totalCoursesPlayed(Region region) {
        return rounds().stream().filter(r -> r.course().region().equals(region)).count();
    }

    public long totalCoursesToBePlayed(Region region) {
        return totalCourseCount(region) - totalCoursesPlayed(region);
    }

    public int overallProgress() {
        return (int) (totalCoursesPlayed() * 100 / totalCourseCount());
    }

}
