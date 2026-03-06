package golf.course;

import org.junit.jupiter.api.Test;

import com.teggr.j2html.preview.Preview;

import j2html.tags.DomContent;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PlayAndStayPageTest {

    @Test
    @Preview
    public DomContent courseGridExample() {
        // Given - courses with play and stay
        List<Course> courses = List.of(
                new Course("Rudding Park Golf Club", Regions.NorthYorkshire, "https://www.ruddingpark.com/", "/images/courses/rudding-park-golf-club.png", false, true),
                new Course("Hollins Hall Hotel & Country Club", Regions.WestYorkshire, "https://www.britanniahotels.com/hotels/hollins-hall-hotel-country-club/golf", "/images/courses/hollins-hall-hotel-country-club.jpg", false, true)
        );

        // When - render course grid
        var result = PlayAndStayPage.courseGrid(courses);

        // Then - should render successfully
        assertNotNull(result);
        String html = result.render();
        assertTrue(html.contains("Rudding Park Golf Club"));
        assertTrue(html.contains("Hollins Hall Hotel &amp; Country Club"));
        assertTrue(html.contains("ygl-card--course"));

        return result;
    }

    @Test
    @Preview
    public DomContent courseGridEmptyExample() {
        // Given - no play and stay courses
        List<Course> courses = List.of();

        // When - render empty grid
        var result = PlayAndStayPage.courseGrid(courses);

        // Then - should render empty state
        assertNotNull(result);
        String html = result.render();
        assertTrue(html.contains("No Play &amp; Stay courses found."));

        return result;
    }

}
