package golf.web;

import golf.course.Course;
import golf.course.Regions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HomePageTest {

    @Test
    void renderShowsRegionCardsWithRandomCourseDetailsAndAnchoredBrowseLinks() throws Exception {
        HomePage homePage = new HomePage();

        Course featuredCourse = new Course(
                "Featured Club",
                Regions.NorthYorkshire,
                "https://featured.example",
                "/images/featured.jpg",
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

        List<Course> courses = List.of(
                new Course("Ganton Golf Club", Regions.NorthYorkshire, "https://www.gantongolfclub.com", "/images/ganton.jpg", null, false, false, null, null, null, null, null, null, null, null),
                new Course("Bridlington Links", Regions.EastYorkshire, "https://bridlington.example", null, null, false, false, null, null, null, null, null, null, null, null),
                new Course("Rother Valley Golf", Regions.SouthYorkshire, "https://rothervalley.example", "/images/rother.jpg", null, false, false, null, null, null, null, null, null, null, null),
                new Course("Moortown Golf Club", Regions.WestYorkshire, "https://www.moortowngc.co.uk", "/images/moortown.jpg", null, false, false, null, null, null, null, null, null, null, null)
        );

        // Render through view to validate full home markup
        var request = new org.springframework.mock.web.MockHttpServletRequest();
        var response = new org.springframework.mock.web.MockHttpServletResponse();
        homePage.render(Map.of("featuredCourse", featuredCourse, "courses", courses), request, response);
        String body = response.getContentAsString();
        String contentType = response.getContentType();

        assertNotNull(contentType);
        assertTrue(contentType.startsWith("text/html"));

        assertTrue(body.contains("Courses by Region"));
        assertTrue(body.contains("North Yorkshire"));
        assertTrue(body.contains("East Yorkshire"));
        assertTrue(body.contains("South Yorkshire"));
        assertTrue(body.contains("West Yorkshire"));

        assertFalse(body.contains(" courses</p>"));

        assertTrue(body.contains("/courses#north-yorkshire"));
        assertTrue(body.contains("/courses#east-yorkshire"));
        assertTrue(body.contains("/courses#south-yorkshire"));
        assertTrue(body.contains("/courses#west-yorkshire"));
        assertTrue(body.contains("href=\"/courses#north-yorkshire\" class=\"ygl-card ygl-card--stat\""));
        assertTrue(body.contains("href=\"/courses#east-yorkshire\" class=\"ygl-card ygl-card--stat\""));
        assertTrue(body.contains("href=\"/courses#south-yorkshire\" class=\"ygl-card ygl-card--stat\""));
        assertTrue(body.contains("href=\"/courses#west-yorkshire\" class=\"ygl-card ygl-card--stat\""));
        assertTrue(body.contains("ygl-card__media"));
        assertTrue(body.contains("ygl-card__img"));
        assertTrue(body.contains("ygl-card__placeholder"));
        assertTrue(body.contains("row g-4"));
        assertTrue(body.contains("col-6 col-lg-3"));
        assertFalse(body.contains("Sign in to track your challenge"));
        assertFalse(body.contains("Create an account"));
        assertFalse(body.contains("Forgot password?"));

        assertTrue(
                body.contains("href=\"/courses\" class=\"nav-link ygl-navbar__link\"")
                        || body.contains("class=\"nav-link ygl-navbar__link\" href=\"/courses\"")
        );
        assertTrue(
                body.contains("href=\"/challenge\" class=\"nav-link ygl-navbar__link\"")
                        || body.contains("class=\"nav-link ygl-navbar__link\" href=\"/challenge\"")
        );
        assertFalse(body.contains("ygl-navbar__link--active"));
        assertFalse(body.contains("aria-current=\"page\""));

        assertTrue(body.contains("/courses/ganton-golf-club")
                || body.contains("/courses/bridlington-links")
                || body.contains("/courses/rother-valley-golf")
                || body.contains("/courses/moortown-golf-club"));
    }
}
