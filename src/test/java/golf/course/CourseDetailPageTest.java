package golf.course;

import org.junit.jupiter.api.Test;

import j2html.tags.DomContent;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CourseDetailPageTest {

    @Test
    void renderHighlightsCoursesNavItemForCourseDetailPath() throws Exception {
        CourseDetailPage page = new CourseDetailPage();

        Course course = new Course(
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
                null,
                null,
                null,
                null,
                null,
                null
        );

        var request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setRequestURI("/courses/alwoodley-golf-club");
        var response = new org.springframework.mock.web.MockHttpServletResponse();

        page.render(java.util.Map.of("course", course), request, response);

        String html = response.getContentAsString();
        assertTrue(
            html.contains("href=\"/courses\" class=\"nav-link ygl-navbar__link ygl-navbar__link--active\"")
                || html.contains("class=\"nav-link ygl-navbar__link ygl-navbar__link--active\" href=\"/courses\"")
        );
        assertFalse(
            html.contains("href=\"/challenge\" class=\"nav-link ygl-navbar__link ygl-navbar__link--active\"")
                || html.contains("class=\"nav-link ygl-navbar__link ygl-navbar__link--active\" href=\"/challenge\"")
        );
    }

    @Test
    void toCourseSlugConvertsNameToUrlSlug() {
        assertEquals("alwoodley-golf-club", Courses.toCourseSlug("Alwoodley Golf Club"));
        assertEquals("ganton-golf-club", Courses.toCourseSlug("Ganton Golf Club"));
        assertEquals("rudding-park-golf-club", Courses.toCourseSlug("Rudding Park Golf Club"));
        assertEquals("sand-moor-golf-club", Courses.toCourseSlug("Sand Moor Golf Club"));
    }

    @Test
    void courseDetailRendersNameRegionWebsiteAddressAndMapWidget() {
        Course course = new Course(
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
            null,
            null,
            null,
            null,
            null,
            null
        );

        DomContent result = CourseDetailPage.courseDetail(course);

        assertNotNull(result);
        String html = result.render();
        assertTrue(html.contains("Alwoodley Golf Club"));
        assertTrue(html.contains("West Yorkshire"));
        assertTrue(html.contains("https://alwoodleygolfclub.com/"));
        assertTrue(html.contains("Visit website"));
        assertFalse(html.contains("/courses/alwoodley-golf-club/tee-times"));
        assertTrue(html.contains("Address"));
        assertTrue(html.contains("Alwoodley Ln, Leeds LS17 7DJ"));
        assertTrue(html.contains("Google Maps"));
        assertTrue(html.contains("iframe"));
        assertTrue(html.contains("ygl-course-map"));
        assertTrue(html.contains("Alwoodley+Golf+Club%2C+Alwoodley+Ln%2C+Leeds+LS17+7DJ"));
        assertTrue(html.contains("output=embed") || html.contains("maps/embed/v1/place"));
        assertTrue(html.contains("ygl-article"));
    }

    @Test
    void courseDetailRendersPlayAndStayBadge() {
        Course course = new Course(
                "Rudding Park Golf Club",
                Regions.NorthYorkshire,
                "https://www.ruddingpark.com/",
                "/images/courses/rudding-park-golf-club.jpg",
                null,
                false,
            true,
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

        DomContent result = CourseDetailPage.courseDetail(course);

        assertNotNull(result);
        String html = result.render();
        assertTrue(html.contains("Play &amp; Stay"));
        assertTrue(html.contains("bi-house-door-fill"));
    }

    @Test
    void courseDetailRendersWithoutWebsite() {
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

        DomContent result = CourseDetailPage.courseDetail(course);

        assertNotNull(result);
        String html = result.render();
        assertTrue(html.contains("Sand Moor Golf Club"));
        assertTrue(html.contains("West Yorkshire"));
        assertFalse(html.contains("Visit website"));
        assertFalse(html.contains("Tee times"));
    }

    @Test
    void courseDetailRendersTeeTimesLinkWhenAvailable() {
        Course course = new Course(
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
                null,
                null,
                null,
                null,
                null,
                "https://www.golfnow.co.uk/tee-times/facility/123-alwoodley-golf-club"
        );

        DomContent result = CourseDetailPage.courseDetail(course);

        assertNotNull(result);
        String html = result.render();
        assertTrue(html.contains("/courses/alwoodley-golf-club/tee-times"));
        assertTrue(html.contains("Tee times"));
    }

        @Test
        void courseDetailRendersNearbyCoursesSectionAfterMapWithCards() {
        Course course = new Course(
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
            "Sand Moor Golf Club",
            "Leeds Golf Centre (Wike Ridge)",
            null,
            null,
            null
        );

        List<Course> nearbyCourses = List.of(
            new Course(
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
                null,
                null
            ),
            new Course(
                "Sand Moor Golf Club",
                Regions.WestYorkshire,
                "https://www.sandmoorgolf.co.uk/",
                "/images/courses/sand-moor-golf-club.jpg",
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
            ),
            new Course(
                "Leeds Golf Centre (Wike Ridge)",
                Regions.WestYorkshire,
                "https://www.leedsgolfcentre.com/",
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
            )
        );

        DomContent result = CourseDetailPage.courseDetail(course, "", nearbyCourses);

        assertNotNull(result);
        String html = result.render();
        assertTrue(html.contains("Google Maps"));
        assertTrue(html.contains("Nearby courses"));
        assertTrue(html.indexOf("Google Maps") < html.indexOf("Nearby courses"));
        assertTrue(html.contains("Moortown Golf Club"));
        assertTrue(html.contains("Sand Moor Golf Club"));
        assertTrue(html.contains("Leeds Golf Centre (Wike Ridge)"));
        assertTrue(html.contains("View course"));
        assertTrue(html.contains("/courses/moortown-golf-club"));
        assertTrue(html.contains("/courses/sand-moor-golf-club"));
        assertTrue(html.contains("/courses/leeds-golf-centre-wike-ridge"));
        assertTrue(html.contains("ygl-card"));
        assertTrue(html.contains("ygl-card__media"));
        assertTrue(html.contains("ygl-card__img"));
        assertTrue(html.contains("ygl-card__placeholder"));
        }

        @Test
        void courseDetailDoesNotRenderNearbyCoursesSectionWhenNoneProvided() {
        Course course = new Course(
            "Sand Moor Golf Club",
            Regions.WestYorkshire,
            null,
            null,
            null,
            false,
            false,
            "Claw Hall Rd, Alwoodley, Leeds LS17 8AB",
            53.86088,
            -1.53362,
            null,
            null,
            null,
            null,
            null,
            null
        );

        DomContent result = CourseDetailPage.courseDetail(course, "", List.of());

        assertNotNull(result);
        String html = result.render();
        assertFalse(html.contains("Nearby courses"));
        }

}
