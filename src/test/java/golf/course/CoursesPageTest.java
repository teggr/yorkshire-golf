package golf.course;

import org.junit.jupiter.api.Test;

import com.teggr.j2html.preview.Preview;

import j2html.tags.DomContent;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CoursesPageTest {

    @Test
    @Preview
    public DomContent coursesListExample() {
        // Given - example courses grouped by region
        Map<Region, List<Course>> byRegion = Map.of(
                Regions.NorthYorkshire, List.of(
                        new Course("Ganton Golf Club", Regions.NorthYorkshire, "https://www.gantongolfclub.com", null, null, false, false, null, null, null, null, null, null, null, null),
                        new Course("Fulford Golf Club", Regions.NorthYorkshire, "https://www.fulfordgolfclub.co.uk", null, null, false, false, null, null, null, null, null, null, null, null)
                ),
                Regions.WestYorkshire, List.of(
                        new Course("Moortown Golf Club", Regions.WestYorkshire, "https://www.moortowngc.co.uk", null, null, false, false, null, null, null, null, null, null, null, null),
                        new Course("Sand Moor Golf Club", Regions.WestYorkshire, null, null, null, false, false, null, null, null, null, null, null, null, null)
                )
        );

        List<Region> regionOrder = List.of(
                Regions.NorthYorkshire,
                Regions.EastYorkshire,
                Regions.SouthYorkshire,
                Regions.WestYorkshire
        );

        // When - render courses list
        var result = CoursesPage.coursesList(byRegion, regionOrder);

        // Then - should render successfully
        assertNotNull(result);
        String html = result.render();
        assertTrue(html.contains("container ygl-page"));
        assertTrue(html.contains("North Yorkshire"));
        assertTrue(html.contains("Ganton Golf Club"));
        assertTrue(html.contains("Moortown Golf Club"));
        assertTrue(html.contains("id=\"north-yorkshire\""));
        assertTrue(html.contains("id=\"east-yorkshire\""));
        assertTrue(html.contains("id=\"south-yorkshire\""));
        assertTrue(html.contains("id=\"west-yorkshire\""));

        return result;
    }

    @Test
    @Preview
    public DomContent courseCardExample() {
        // Given - a course with a website
        Course courseWithWebsite = new Course(
                "Ganton Golf Club",
                Regions.NorthYorkshire,
                "https://www.gantongolfclub.com",
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

        // When - render course card
        var result = CoursesPage.courseCard(courseWithWebsite);

        // Then - should render successfully and link to course detail page
        assertNotNull(result);
        String html = result.render();
        assertTrue(html.contains("Ganton Golf Club"));
        assertTrue(html.contains("ygl-card--course"));
        assertTrue(html.contains("/courses/ganton-golf-club"));

        return result;
    }

    @Test
    @Preview
    public DomContent courseCardWithoutWebsiteExample() {
        // Given - a course without a website
        Course courseWithoutWebsite = new Course(
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

        // When - render course card
        var result = CoursesPage.courseCard(courseWithoutWebsite);

        // Then - should render successfully and always link to the course detail page,
        // even when no external website is available
        assertNotNull(result);
        String html = result.render();
        assertTrue(html.contains("Sand Moor Golf Club"));
        assertTrue(html.contains("ygl-card--course"));
        assertTrue(html.contains("ygl-card__media"));
        assertTrue(html.contains("ygl-card__placeholder"));
        assertTrue(html.contains("/courses/sand-moor-golf-club"));
        assertFalse(html.contains("Visit website"));

        return result;
    }

    @Test
    @Preview
    public DomContent courseCardWithImageExample() {
        // Given - a course with a website and main image
        Course courseWithImage = new Course(
                "Rudding Park Golf Club",
                Regions.NorthYorkshire,
                "https://www.ruddingpark.com/",
                "/images/courses/rudding-park-golf-club.jpg",
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

        // When - render course card
        var result = CoursesPage.courseCard(courseWithImage);

        // Then - should render successfully with image and link to course detail page
        assertNotNull(result);
        String html = result.render();
        assertTrue(html.contains("Rudding Park Golf Club"));
        assertTrue(html.contains("ygl-card--course"));
        assertTrue(html.contains("ygl-card__media"));
        assertTrue(html.contains("ygl-card__img"));
        assertTrue(html.contains("/images/courses/rudding-park-golf-club.jpg"));
        assertTrue(html.contains("/courses/rudding-park-golf-club"));

        return result;
    }

        @Test
        void toRegionSlugMapsDisplayNameToAnchorSlug() {
                assertEquals("north-yorkshire", CoursesPage.toRegionSlug(Regions.NorthYorkshire));
                assertEquals("east-yorkshire", CoursesPage.toRegionSlug(Regions.EastYorkshire));
                assertEquals("south-yorkshire", CoursesPage.toRegionSlug(Regions.SouthYorkshire));
                assertEquals("west-yorkshire", CoursesPage.toRegionSlug(Regions.WestYorkshire));
        }

    @Test
    @Preview
    public DomContent courseCardWithPlayAndStayExample() {
        // Given - a course with play and stay
        Course courseWithPlayAndStay = new Course(
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

        // When - render course card
        var result = CoursesPage.courseCard(courseWithPlayAndStay);

        // Then - should render with hotel icon
        assertNotNull(result);
        String html = result.render();
        assertTrue(html.contains("Rudding Park Golf Club"));
        assertTrue(html.contains("bi bi-house-door-fill"));
        assertTrue(html.contains("Play &amp; Stay"));

        return result;
    }
}
