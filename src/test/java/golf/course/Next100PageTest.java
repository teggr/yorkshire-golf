package golf.course;

import org.junit.jupiter.api.Test;

import com.teggr.j2html.preview.Preview;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Next100PageTest {

    @Test
    @Preview
    void next100SectionUsesTop100LayoutExample() {
        Course course = new Course(
                "Hallowes Golf Club",
                Regions.SouthYorkshire,
                "https://www.hallowesgolfclub.co.uk",
                "/images/courses/hallowes-golf-club.jpg",
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
                true
        );

        var result = Next100Page.courseSection(course);

        assertNotNull(result);
        String html = result.render();
        assertTrue(html.contains("ygl-top100-entry"));
        assertTrue(html.contains("ygl-top100-entry__rank-label"));
        assertTrue(html.contains("ygl-top100-entry__image-wrapper"));
        assertTrue(html.contains("ygl-top100-entry__title"));
        assertTrue(html.contains("Golf Monthly Next 100"));
        assertTrue(!html.contains("#132"));

    }
}
