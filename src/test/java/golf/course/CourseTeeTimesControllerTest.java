package golf.course;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseTeeTimesControllerTest {

	@Test
	void teeTimesRedirectsToGolfNowUrl() {
		Courses courses = mock(Courses.class);
		CourseTeeTimesController controller = new CourseTeeTimesController(courses);

		Course course = new Course(
				"Alwoodley Golf Club",
				Regions.WestYorkshire,
				"https://alwoodleygolfclub.com/",
				"/images/courses/alwoodley-golf-club.jpg",
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
				null,
				"https://www.golfnow.co.uk/tee-times/facility/123-alwoodley-golf-club"
		);

		when(courses.getCourseBySlug("alwoodley-golf-club")).thenReturn(course);

		assertEquals("redirect:https://www.golfnow.co.uk/tee-times/facility/123-alwoodley-golf-club", controller.teeTimes("alwoodley-golf-club"));
	}

	@Test
	void teeTimesReturnsNotFoundWhenCourseHasNoGolfNowUrl() {
		Courses courses = mock(Courses.class);
		CourseTeeTimesController controller = new CourseTeeTimesController(courses);

		Course course = new Course(
				"Sand Moor Golf Club",
				Regions.WestYorkshire,
				null,
				null,
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
				null,
				null
		);

		when(courses.getCourseBySlug("sand-moor-golf-club")).thenReturn(course);

		assertThrows(ResponseStatusException.class, () -> controller.teeTimes("sand-moor-golf-club"));
	}
}