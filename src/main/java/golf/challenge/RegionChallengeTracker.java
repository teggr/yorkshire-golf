package golf.challenge;

import golf.course.Course;
import golf.course.Region;

import java.util.List;
import java.util.Map;

public record RegionChallengeTracker(
        Map<Region, Long> courseCountByRegion,
        List<Course> playedCourses
) {

    public long totalCourseCount() {
        return courseCountByRegion.values().stream()
                .reduce(0L, Long::sum);
    }

    public long totalCoursesPlayed() {
        return playedCourses().size();
    }

    public long totalCoursesToBePlayed() {
        return totalCourseCount() - totalCoursesPlayed();
    }

    public long totalCourseCount(Region region) {
        return courseCountByRegion.get(region);
    }

    public long totalCoursesPlayed(Region region) {
        return playedCourses().stream().filter(c -> c.region().equals(region)).count();
    }

    public long totalCoursesToBePlayed(Region region) {
        return totalCourseCount(region) - totalCoursesPlayed(region);
    }

    public int overallProgress() {
        if (totalCourseCount() == 0) return 0;
        return (int) (totalCoursesPlayed() * 100 / totalCourseCount());
    }

}
