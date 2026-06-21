package golf.challenge;

import golf.course.Course;
import golf.course.Regions;
import golf.user.UserRound;
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
                        new Course("Ganton Golf Club", Regions.NorthYorkshire, "https://www.gantongolfclub.com", null, null, false, false, null, null, null, null, null, null, null, null, null)
                )
        ), request, response);

        String html = response.getContentAsString();
        assertTrue(
                html.contains("href=\"/challenge\" class=\"nav-link ygl-navbar__link ygl-navbar__link--active\"")
                        || html.contains("class=\"nav-link ygl-navbar__link ygl-navbar__link--active\" href=\"/challenge\"")
        );
        assertTrue(html.contains("id=\"progressLineChart\""));
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

    @Test
    void renderShowsLoggedRoundsSectionAndActionsForTrackerOwner() throws Exception {
        RegionTrackerPage page = new RegionTrackerPage();

        Course course = new Course("Ganton Golf Club", Regions.NorthYorkshire, "https://www.gantongolfclub.com", null, null, false, false, null, null, null, null, null, null, null, null, null);
        RegionChallengeTracker tracker = new RegionChallengeTracker(
                Map.of(
                        Regions.NorthYorkshire, 10L,
                        Regions.EastYorkshire, 20L,
                        Regions.SouthYorkshire, 30L,
                        Regions.WestYorkshire, 40L
                ),
                List.of(new UserRound("123", 10L, null, "2026-03-07", "Ganton Golf Club", course, null, null))
        );

        var request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setRequestURI("/challenge/abc123tracker");
        var response = new org.springframework.mock.web.MockHttpServletResponse();

        page.render(Map.of(
                "tracker", tracker,
                "trackerId", "abc123tracker",
                "canAddRound", true,
                "allCourses", List.of(course)
        ), request, response);

        String html = response.getContentAsString();
        assertTrue(html.contains("Your Played Courses"));
        assertTrue(html.contains("id=\"progressLineChart\""));
        assertTrue(html.contains("/challenge/abc123tracker/update-round-date"));
        assertTrue(html.contains("/challenge/abc123tracker/delete-round"));
        assertTrue(html.contains("Save date"));
        assertTrue(html.contains("Delete"));
        assertTrue(html.contains("Are you sure you want to delete this round?"));
        assertTrue(html.contains("ygl-share-banner"));
        assertTrue(html.contains("courseName,date"));
        assertTrue(html.contains("bulk-import"));
    }

    @Test
    void renderShowsSharingBannerForNonOwner() throws Exception {
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
                "allCourses", List.of()
        ), request, response);

        String html = response.getContentAsString();
        assertTrue(html.contains("ygl-share-banner"));
        assertTrue(html.contains("This page is public"));
        assertTrue(html.contains("ygl-download-infographics"));
        assertTrue(html.contains("Download Infographics"));
        assertTrue(html.contains("download-infographics.js"));
        assertFalse(html.contains("courseName,date"));
        assertFalse(html.contains("Import Rounds from CSV"));
    }

    @Test
    void renderHidesLoggedRoundsSectionForNonOwner() throws Exception {
        RegionTrackerPage page = new RegionTrackerPage();

        Course course = new Course("Ganton Golf Club", Regions.NorthYorkshire, "https://www.gantongolfclub.com", null, null, false, false, null, null, null, null, null, null, null, null, null);
        RegionChallengeTracker tracker = new RegionChallengeTracker(
                Map.of(
                        Regions.NorthYorkshire, 10L,
                        Regions.EastYorkshire, 20L,
                        Regions.SouthYorkshire, 30L,
                        Regions.WestYorkshire, 40L
                ),
                List.of(new UserRound("123", 10L, null, "2026-03-07", "Ganton Golf Club", course, null, null))
        );

        var request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setRequestURI("/challenge/abc123tracker");
        var response = new org.springframework.mock.web.MockHttpServletResponse();

        page.render(Map.of(
                "tracker", tracker,
                "trackerId", "abc123tracker",
                "canAddRound", false,
                "allCourses", List.of(course)
        ), request, response);

        String html = response.getContentAsString();
        assertTrue(html.contains("id=\"progressLineChart\""));
        assertFalse(html.contains("Your Played Courses"));
        assertFalse(html.contains("/challenge/abc123tracker/update-round-date"));
        assertFalse(html.contains("/challenge/abc123tracker/delete-round"));
    }
}
