package golf.course;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoursesTest {

    @Test
    void getNext100CoursesReturnsOnlyTrueAndAlphabetical() throws Exception {
        Courses courses = new Courses();

        Field coursesField = Courses.class.getDeclaredField("courses");
        coursesField.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Course> internalCourses = (List<Course>) coursesField.get(courses);

        internalCourses.addAll(List.of(
                new Course("zeta club", Regions.NorthYorkshire, null, null, null, false, false, null, null, null, null, null, null, null, true, null),
                new Course("Alpha Club", Regions.NorthYorkshire, null, null, null, false, false, null, null, null, null, null, null, null, true, null),
                new Course("bravo club", Regions.NorthYorkshire, null, null, null, false, false, null, null, null, null, null, null, null, false, null),
                new Course("charlie club", Regions.NorthYorkshire, null, null, null, true, false, null, null, null, null, null, null, null, true, null),
                new Course("delta club", Regions.NorthYorkshire, null, null, null, false, false, null, null, null, null, null, null, null, null, null),
                new Course("beta club", Regions.NorthYorkshire, null, null, null, false, false, null, null, null, null, null, null, null, true, null)
        ));

        List<String> next100CourseNames = courses.getNext100Courses().stream()
                .map(Course::name)
                .toList();

        assertEquals(List.of("Alpha Club", "beta club", "zeta club"), next100CourseNames);
    }
}
