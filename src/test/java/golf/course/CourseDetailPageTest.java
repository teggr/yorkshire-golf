package golf.course;

import org.junit.jupiter.api.Test;

import j2html.tags.DomContent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CourseDetailPageTest {

    @Test
    void toCourseSlugConvertsNameToUrlSlug() {
        assertEquals("alwoodley-golf-club", Courses.toCourseSlug("Alwoodley Golf Club"));
        assertEquals("ganton-golf-club", Courses.toCourseSlug("Ganton Golf Club"));
        assertEquals("rudding-park-golf-club", Courses.toCourseSlug("Rudding Park Golf Club"));
        assertEquals("sand-moor-golf-club", Courses.toCourseSlug("Sand Moor Golf Club"));
    }

    @Test
    void courseDetailRendersNameRegionAndWebsite() {
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
            null
        );

        DomContent result = CourseDetailPage.courseDetail(course);

        assertNotNull(result);
        String html = result.render();
        assertTrue(html.contains("Alwoodley Golf Club"));
        assertTrue(html.contains("West Yorkshire"));
        assertTrue(html.contains("https://alwoodleygolfclub.com/"));
        assertTrue(html.contains("Visit website"));
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
            null
        );

        DomContent result = CourseDetailPage.courseDetail(course);

        assertNotNull(result);
        String html = result.render();
        assertTrue(html.contains("Sand Moor Golf Club"));
        assertTrue(html.contains("West Yorkshire"));
        assertFalse(html.contains("Visit website"));
    }

}
