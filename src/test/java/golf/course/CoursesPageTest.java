package golf.course;

import org.junit.jupiter.api.Test;

import com.teggr.j2html.preview.Preview;

import j2html.tags.DomContent;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CoursesPageTest {

    @Test
    @Preview
    public DomContent coursesListExample() {
        // Given - example courses grouped by region
        Map<Region, List<Course>> byRegion = Map.of(
                Regions.NorthYorkshire, List.of(
                        new Course("Ganton Golf Club", Regions.NorthYorkshire, "https://www.gantongolfclub.com", null, false),
                        new Course("Fulford Golf Club", Regions.NorthYorkshire, "https://www.fulfordgolfclub.co.uk", null, false)
                ),
                Regions.WestYorkshire, List.of(
                        new Course("Moortown Golf Club", Regions.WestYorkshire, "https://www.moortowngc.co.uk", null, false),
                        new Course("Sand Moor Golf Club", Regions.WestYorkshire, null, null, false)
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
                false
        );

        // When - render course card
        var result = CoursesPage.courseCard(courseWithWebsite);

        // Then - should render successfully
        assertNotNull(result);
        String html = result.render();
        assertTrue(html.contains("Ganton Golf Club"));
        assertTrue(html.contains("https://www.gantongolfclub.com"));
        assertTrue(html.contains("ygl-card--course"));

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
                false
        );

        // When - render course card
        var result = CoursesPage.courseCard(courseWithoutWebsite);

        // Then - should render successfully
        assertNotNull(result);
        String html = result.render();
        assertTrue(html.contains("Sand Moor Golf Club"));
        assertTrue(html.contains("ygl-card--course"));
        assertFalse(html.contains("href"));

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
                false
        );

        // When - render course card
        var result = CoursesPage.courseCard(courseWithImage);

        // Then - should render successfully with image
        assertNotNull(result);
        String html = result.render();
        assertTrue(html.contains("Rudding Park Golf Club"));
        assertTrue(html.contains("ygl-card--course"));
        assertTrue(html.contains("ygl-card__img"));
        assertTrue(html.contains("/images/courses/rudding-park-golf-club.jpg"));

        return result;
    }
}
