package golf;

import java.util.List;

public record CourseTracker(
        java.util.Map<golf.Region, Integer> courseCountByRegion,
        List<CourseRecord> courses
) {

    int totalCourseCount() {
        return courseCountByRegion.values().stream()
                .reduce(0, Integer::sum);
    }

    int totalCoursesPlayed() {
        return courses().size();
    }

    int totalCoursesToBePlayed() {
        return totalCourseCount() - totalCoursesPlayed();
    }

    int totalCourseCount(Region region) {
        return courseCountByRegion.get(region);
    }

    int totalCoursesPlayed(Region region) {
        return (int) courses().stream().filter(cr -> cr.course().region().equals(region)).count();
    }

    int totalCoursesToBePlayed(Region region) {
        return totalCourseCount(region) - totalCoursesPlayed(region);
    }

    public int overallProgress() {
        return totalCoursesPlayed() * 100 / totalCourseCount();
    }

}
