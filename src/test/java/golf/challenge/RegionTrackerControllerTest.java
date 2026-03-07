package golf.challenge;

import golf.course.Course;
import golf.course.Courses;
import golf.user.GolfUser;
import golf.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegionTrackerControllerTest {

    @Test
    void anonymousUserGetsChallengeLandingPage() {
        UserService userService = mock(UserService.class);
        Courses courses = mock(Courses.class);
        when(courses.getAllCourses()).thenReturn(List.of(mock(Course.class), mock(Course.class)));

        RegionTrackerController controller = new RegionTrackerController(userService, courses);
        Model model = new ExtendedModelMap();

        String view = controller.home(null, model);

        assertEquals("challengeLandingPage", view);
        assertEquals(2, model.getAttribute("totalCourseCount"));
    }

    @Test
    void loggedInUserRedirectsToOwnChallengePath() {
        UserService userService = mock(UserService.class);
        Courses courses = mock(Courses.class);
        RegionTrackerController controller = new RegionTrackerController(userService, courses);
        Model model = new ExtendedModelMap();

        UserDetails userDetails = User.withUsername("golfer@example.com")
                .password("unused")
                .roles("USER")
                .build();

        when(userService.findByEmail("golfer@example.com"))
                .thenReturn(Optional.of(new GolfUser(1L, "golfer@example.com", "secret", "abc123tracker", "USER", false)));

        String view = controller.home(userDetails, model);

        assertEquals("redirect:/challenge/abc123tracker", view);
    }

    @Test
    void unknownLoggedInUserRedirectsToLogin() {
        UserService userService = mock(UserService.class);
        Courses courses = mock(Courses.class);
        RegionTrackerController controller = new RegionTrackerController(userService, courses);
        Model model = new ExtendedModelMap();

        UserDetails userDetails = User.withUsername("missing@example.com")
                .password("unused")
                .roles("USER")
                .build();

        when(userService.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        String view = controller.home(userDetails, model);

        assertEquals("redirect:/login", view);
    }
}