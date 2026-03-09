package golf.course;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchControllerTest {

    @Test
    void searchReturnsResultsPageWhenZeroResults() {
        Courses courses = mock(Courses.class);
        SearchController controller = new SearchController(courses);
        Model model = new ExtendedModelMap();

        when(courses.search("zzz")).thenReturn(List.of());

        String view = controller.search("zzz", model);

        assertEquals("searchResultsPage", view);
        assertEquals("zzz", model.getAttribute("query"));
        assertEquals(0, model.getAttribute("resultCount"));
    }

    @Test
    void searchRedirectsDirectlyWhenSingleMatch() {
        Courses courses = mock(Courses.class);
        SearchController controller = new SearchController(courses);
        Model model = new ExtendedModelMap();
        Course lindrick = new Course("Lindrick Golf Club", Regions.SouthYorkshire, null, null, null, false, false,
                "Lindrick Dale, Worksop", null, null, null, null, null, null, null);

        when(courses.search("lindrick")).thenReturn(List.of(lindrick));

        String view = controller.search("lindrick", model);

        assertEquals("redirect:/courses/lindrick-golf-club", view);
        verify(courses, never()).getAllCourses();
    }

    @Test
    void searchReturnsResultsPageWhenMultipleMatches() {
        Courses courses = mock(Courses.class);
        SearchController controller = new SearchController(courses);
        Model model = new ExtendedModelMap();

        List<Course> matches = List.of(
                new Course("Leeds Golf Club", Regions.WestYorkshire, null, null, null, false, false,
                        "Elmete Lane, Leeds", null, null, null, null, null, null, null),
                new Course("Leeds West Club", Regions.WestYorkshire, null, null, null, false, false,
                        "Leeds", null, null, null, null, null, null, null)
        );
        when(courses.search("leeds")).thenReturn(matches);

        String view = controller.search("leeds", model);

        assertEquals("searchResultsPage", view);
        assertEquals("leeds", model.getAttribute("query"));
        assertEquals(2, model.getAttribute("resultCount"));
        assertEquals(matches, model.getAttribute("results"));
    }
}
