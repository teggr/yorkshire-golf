package golf.web;

import golf.course.Course;
import golf.course.Regions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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
                false
        );

        List<Course> courses = List.of(
                new Course("Ganton Golf Club", Regions.NorthYorkshire, "https://www.gantongolfclub.com", "/images/ganton.jpg", null, false, false),
                new Course("Bridlington Links", Regions.EastYorkshire, "https://bridlington.example", null, null, false, false),
                new Course("Rother Valley Golf", Regions.SouthYorkshire, "https://rothervalley.example", "/images/rother.jpg", null, false, false),
                new Course("Moortown Golf Club", Regions.WestYorkshire, "https://www.moortowngc.co.uk", "/images/moortown.jpg", null, false, false)
        );

        // Render through view to validate full home markup
        var response = new org.springframework.mock.web.MockHttpServletResponse();
        homePage.render(Map.of("featuredCourse", featuredCourse, "courses", courses), null, response);
        String body = response.getContentAsString();

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
        assertTrue(body.contains("ygl-card__media"));
        assertTrue(body.contains("ygl-card__img"));
        assertTrue(body.contains("ygl-card__placeholder"));

        assertTrue(body.contains("/courses/ganton-golf-club")
                || body.contains("/courses/bridlington-links")
                || body.contains("/courses/rother-valley-golf")
                || body.contains("/courses/moortown-golf-club"));
    }
}
