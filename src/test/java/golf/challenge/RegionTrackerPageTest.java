package golf.challenge;

import golf.course.Course;
import golf.course.Regions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionTrackerPageTest {

    @Test
    void renderHighlightsChallengeNavItemForTrackerPath() throws Exception {
        RegionTrackerPage page = new RegionTrackerPage();

        RegionChallengeTracker tracker = new RegionChallengeTracker(
                Map.of(
                        Regions.NorthYorkshire, 10L,
                        Regions.EastYorkshire, 20L,
                        Regions.SouthYorkshire, 30L,
                        Regions.WestYorkshire, 40L
                ),
                List.of()
        );

        var request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setRequestURI("/challenge/abc123tracker");
        var response = new org.springframework.mock.web.MockHttpServletResponse();

        page.render(Map.of(
                "tracker", tracker,
                "trackerId", "abc123tracker",
                "canAddRound", false,
                "allCourses", List.of(
                        new Course("Ganton Golf Club", Regions.NorthYorkshire, "https://www.gantongolfclub.com", null, null, false, false, null, null, null, null, null, null, null, null)
                )
        ), request, response);

        String html = response.getContentAsString();
        assertTrue(
                html.contains("href=\"/challenge\" class=\"nav-link ygl-navbar__link ygl-navbar__link--active\"")
                        || html.contains("class=\"nav-link ygl-navbar__link ygl-navbar__link--active\" href=\"/challenge\"")
        );
        assertTrue(
                html.contains("href=\"/challenge\" class=\"nav-link ygl-navbar__link ygl-navbar__link--active\" aria-current=\"page\"")
                        || html.contains("class=\"nav-link ygl-navbar__link ygl-navbar__link--active\" href=\"/challenge\" aria-current=\"page\"")
                        || html.contains("class=\"nav-link ygl-navbar__link ygl-navbar__link--active\" aria-current=\"page\" href=\"/challenge\"")
        );
        assertFalse(
                html.contains("href=\"/courses\" class=\"nav-link ygl-navbar__link ygl-navbar__link--active\"")
                        || html.contains("class=\"nav-link ygl-navbar__link ygl-navbar__link--active\" href=\"/courses\"")
        );
    }
}
