package golf.tracker;

import java.util.List;

public record CourseTracker(
        java.util.Map<Region, Integer> courseCountByRegion,
        List<CourseRecord> courses
) {

    public int totalCourseCount() {
        return courseCountByRegion.values().stream()
                .reduce(0, Integer::sum);
    }

    public int totalCoursesPlayed() {
        return courses().size();
    }

    public int totalCoursesToBePlayed() {
        return totalCourseCount() - totalCoursesPlayed();
    }

    public int totalCourseCount(Region region) {
        return courseCountByRegion.get(region);
    }

    public int totalCoursesPlayed(Region region) {
        return (int) courses().stream().filter(cr -> cr.course().region().equals(region)).count();
    }

    public int totalCoursesToBePlayed(Region region) {
        return totalCourseCount(region) - totalCoursesPlayed(region);
    }

    public int overallProgress() {
        return totalCoursesPlayed() * 100 / totalCourseCount();
    }

}
