package golf.course;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoursesSearchTest {

    @Test
    void searchMatchesNameAndAddressCaseInsensitiveAndExcludesClosedCourses() throws Exception {
        Courses courses = new Courses();
        seedCourses(courses,
                new Course("Lindrick Golf Club", Regions.SouthYorkshire, null, null, null, false, false,
                        "Lindrick Dale, Worksop", null, null, null, null, null, null, null),
                new Course("Leeds Golf Club", Regions.WestYorkshire, null, null, null, false, false,
                "Elmete Lane, Leeds, West Yorkshire", null, null, null, null, null, null, null),
                new Course("Closed Leeds Golf", Regions.WestYorkshire, null, null, null, true, false,
                        "Leeds", null, null, null, null, null, null, null)
        );

        List<Course> byName = courses.search("lindrick");
        List<Course> byAddress = courses.search("LEEDS");

        assertEquals(1, byName.size());
        assertEquals("Lindrick Golf Club", byName.get(0).name());
        assertEquals(1, byAddress.size());
        assertEquals("Leeds Golf Club", byAddress.get(0).name());
        assertTrue(byAddress.stream().noneMatch(Course::closed));
    }

    @Test
    void searchMatchesMultiWordTokensEvenWhenNonContiguous() throws Exception {
        Courses courses = new Courses();
        seedCourses(courses,
                new Course("Leeds Golf Club", Regions.WestYorkshire, null, null, null, false, false,
                        "Elmete Lane, Leeds, West Yorkshire", null, null, null, null, null, null, null),
                new Course("York Golf Club", Regions.NorthYorkshire, null, null, null, false, false,
                        "Strensall Road, York", null, null, null, null, null, null, null)
        );

        List<Course> results = courses.search("york leeds");

        assertTrue(results.stream().anyMatch(course -> "Leeds Golf Club".equals(course.name())));
    }

    @Test
    void searchReturnsEmptyForBlankQuery() throws Exception {
        Courses courses = new Courses();
        seedCourses(courses,
                new Course("Lindrick Golf Club", Regions.SouthYorkshire, null, null, null, false, false,
                        "Lindrick Dale, Worksop", null, null, null, null, null, null, null)
        );

        assertTrue(courses.search(" ").isEmpty());
        assertTrue(courses.search(null).isEmpty());
    }

    private static void seedCourses(Courses coursesInstance, Course... values) throws Exception {
        Field coursesField = Courses.class.getDeclaredField("courses");
        coursesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Course> store = (List<Course>) coursesField.get(coursesInstance);
        store.clear();
        store.addAll(List.of(values));
    }
}
