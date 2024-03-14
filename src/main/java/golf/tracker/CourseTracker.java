package golf.tracker;

import java.util.List;

public record CourseTracker(
        java.util.Map<Region, Long> courseCountByRegion,
        List<CourseRecord> courses
) {

    public long totalCourseCount() {
        return courseCountByRegion.values().stream()
                .reduce(0L, Long::sum);
    }

    public long totalCoursesPlayed() {
        return courses().size();
    }

    public long totalCoursesToBePlayed() {
        return totalCourseCount() - totalCoursesPlayed();
    }

    public long totalCourseCount(Region region) {
        return courseCountByRegion.get(region);
    }

    public long totalCoursesPlayed(Region region) {
        return courses().stream().filter(cr -> cr.course().region().equals(region)).count();
    }

    public long totalCoursesToBePlayed(Region region) {
        return totalCourseCount(region) - totalCoursesPlayed(region);
    }

    public int overallProgress() {
        return (int) (totalCoursesPlayed() * 100 / totalCourseCount());
    }

}
