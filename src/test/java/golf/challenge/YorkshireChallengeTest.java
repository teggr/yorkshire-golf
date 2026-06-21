package golf.challenge;

import golf.course.Course;
import golf.course.Courses;
import golf.course.Regions;
import golf.user.UserRound;
import golf.user.UserRoundRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class YorkshireChallengeTest {

    @Test
    void monthlyCourseProgressReturnsEmptySeriesWhenNoRounds() {
        Courses courses = mock(Courses.class);
        UserRoundRepository userRoundRepository = mock(UserRoundRepository.class);
        YorkshireChallenge challenge = new YorkshireChallenge(courses, userRoundRepository);

        when(userRoundRepository.findByUserId(10L)).thenReturn(List.of());
        when(courses.getCourseRegionCountGroupByRegion()).thenReturn(Map.of(
                Regions.NorthYorkshire, 57L,
                Regions.EastYorkshire, 18L,
                Regions.SouthYorkshire, 40L,
                Regions.WestYorkshire, 80L
        ));

        MonthlyCourseProgress progress = challenge.getMonthlyCourseProgressForUser(10L);

        assertEquals(List.of(), progress.labels());
        assertEquals(List.of(), progress.cumulativeCoursesPlayed());
    }

    @Test
    void monthlyCourseProgressBuildsAscendingMonthBucketsWithCumulativeDistinctTotals() {
        Courses courses = mock(Courses.class);
        UserRoundRepository userRoundRepository = mock(UserRoundRepository.class);
        YorkshireChallenge challenge = new YorkshireChallenge(courses, userRoundRepository);

        Course northCourse = new Course("North Course", Regions.NorthYorkshire, null, null, null, false, false, null, null, null, null, null, null, null, null, null);
        Course westCourse = new Course("West Course", Regions.WestYorkshire, null, null, null, false, false, null, null, null, null, null, null, null, null, null);
        Course eastCourse = new Course("East Course", Regions.EastYorkshire, null, null, null, false, false, null, null, null, null, null, null, null, null, null);

        // Repository returns newest-first; service should normalize to ascending timeline.
        when(userRoundRepository.findByUserId(10L)).thenReturn(List.of(
                new UserRound("3", 10L, null, "2024-03-20", "East Course", null, null, null),
                new UserRound("2", 10L, null, "2024-01-15", "West Course", null, null, null),
                new UserRound("1", 10L, null, "2024-01-02", "North Course", null, null, null)
        ));
        when(courses.getCourseByName("North Course")).thenReturn(northCourse);
        when(courses.getCourseByName("West Course")).thenReturn(westCourse);
        when(courses.getCourseByName("East Course")).thenReturn(eastCourse);
        when(courses.getCourseRegionCountGroupByRegion()).thenReturn(Map.of(
                Regions.NorthYorkshire, 57L,
                Regions.EastYorkshire, 18L,
                Regions.SouthYorkshire, 40L,
                Regions.WestYorkshire, 80L
        ));

        MonthlyCourseProgress progress = challenge.getMonthlyCourseProgressForUser(10L);

        assertEquals(List.of("Jan 2024", "Feb 2024", "Mar 2024"), progress.labels());
        assertEquals(List.of(2L, 2L, 3L), progress.cumulativeCoursesPlayed());
    }

}
