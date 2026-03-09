package golf.course;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CoursesControllerTest {

    @Test
    void courseSkipsUnresolvedAndClosedNearbyCourses() {
        Courses courses = mock(Courses.class);
        CoursesController controller = new CoursesController(courses);
        Model model = new ExtendedModelMap();

        Course detailCourse = new Course(
                "Alwoodley Golf Club",
                Regions.WestYorkshire,
                "https://alwoodleygolfclub.com/",
                "/images/courses/alwoodley-golf-club.jpg",
                null,
                false,
                false,
                "Alwoodley Ln, Leeds LS17 7DJ",
                53.85665,
                -1.50115,
                "Moortown Golf Club",
                "Missing Golf Club",
                "Closed Golf Club",
                null,
                null
        );

        Course openNearbyCourse = new Course(
                "Moortown Golf Club",
                Regions.WestYorkshire,
                "https://www.moortowngc.co.uk/",
                "/images/courses/moortown-golf-club.jpg",
                null,
                false,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        Course closedNearbyCourse = new Course(
                "Closed Golf Club",
                Regions.WestYorkshire,
                null,
                null,
                null,
                true,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(courses.getCourseBySlug("alwoodley-golf-club")).thenReturn(detailCourse);
        when(courses.getCourseByName("Moortown Golf Club")).thenReturn(openNearbyCourse);
        when(courses.getCourseByName("Missing Golf Club")).thenThrow(new RuntimeException("not found"));
        when(courses.getCourseByName("Closed Golf Club")).thenReturn(closedNearbyCourse);

        String view = controller.course("alwoodley-golf-club", model);

        assertEquals("courseDetailPage", view);
        assertEquals(detailCourse, model.getAttribute("course"));

        Object nearbyCoursesModel = model.getAttribute("nearbyCourses");
        assertInstanceOf(List.class, nearbyCoursesModel);

        @SuppressWarnings("unchecked")
        List<Course> nearbyCourses = (List<Course>) nearbyCoursesModel;
        assertEquals(1, nearbyCourses.size());
        assertTrue(nearbyCourses.contains(openNearbyCourse));
    }
}
