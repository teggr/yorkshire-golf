package golf.course;

import org.junit.jupiter.api.Test;

import com.teggr.j2html.preview.Preview;

import j2html.tags.DomContent;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PlayAndStayPageTest {

    @Preview
    public DomContent courseGridExample() {
        // Given - courses with play and stay
        List<Course> courses = List.of(
            new Course("Rudding Park Golf Club", Regions.NorthYorkshire, "https://www.ruddingpark.com/", "/images/courses/rudding-park-golf-club.png", "https://example.com/rudding-stay.jpg", false, true, null, null, null, null, null, null, null, null),
            new Course("Hollins Hall Hotel & Country Club", Regions.WestYorkshire, "https://www.britanniahotels.com/hotels/hollins-hall-hotel-country-club/golf", "/images/courses/hollins-hall-hotel-country-club.jpg", null, false, true, null, null, null, null, null, null, null, null)
        );

        return PlayAndStayPage.courseGrid(courses);
    }

    @Test
    public void courseGridRendersPlayAndStayCourses() {
        var result = courseGridExample();

        assertNotNull(result);
        String html = result.render();
        assertTrue(html.contains("Rudding Park Golf Club"));
        assertTrue(html.contains("Hollins Hall Hotel &amp; Country Club"));
        assertTrue(html.contains("ygl-card--course"));
        assertTrue(html.contains("https://example.com/rudding-stay.jpg"));
        assertTrue(html.contains("/images/courses/hollins-hall-hotel-country-club.jpg"));
    }

    @Preview
    public DomContent courseGridEmptyExample() {
        // Given - no play and stay courses
        List<Course> courses = List.of();

        return PlayAndStayPage.courseGrid(courses);
    }

    @Test
    public void courseGridRendersEmptyStateWhenNoCoursesFound() {
        var result = courseGridEmptyExample();

        assertNotNull(result);
        String html = result.render();
        assertTrue(html.contains("No Play &amp; Stay courses found."));
    }

}
