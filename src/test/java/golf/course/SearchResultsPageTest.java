package golf.course;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchResultsPageTest {

    @Test
    void renderShowsFlatResultsWithoutRegionGrouping() throws Exception {
        SearchResultsPage page = new SearchResultsPage();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/search");
        request.setQueryString("q=leeds");
        MockHttpServletResponse response = new MockHttpServletResponse();

        page.render(Map.of(
                "query", "leeds",
                "resultCount", 2,
                "results", List.of(
                        new Course("Leeds Golf Club", Regions.WestYorkshire, null, null, null, false, false,
                                "Elmete Lane, Leeds", null, null, null, null, null, null, null, null),
                        new Course("Leeds West Club", Regions.WestYorkshire, null, null, null, false, false,
                                "Leeds", null, null, null, null, null, null, null, null)
                )
        ), request, response);

        String html = response.getContentAsString();
        assertTrue(html.contains("Search Results"));
        assertTrue(html.contains("2 matches for &quot;leeds&quot;"));
        assertTrue(html.contains("/courses/leeds-golf-club"));
        assertTrue(html.contains("/courses/leeds-west-club"));
                assertTrue(html.contains("View course"));
                assertTrue(html.contains("class=\"ygl-btn ygl-btn--primary ygl-btn--sm\""));
        assertFalse(html.contains("Jump to region"));
        assertFalse(html.contains("id=\"north-yorkshire\""));
    }

    @Test
    void renderShowsZeroResultsStateWithSearchAndExplore() throws Exception {
        SearchResultsPage page = new SearchResultsPage();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/search");
        request.setQueryString("q=zzzz");
        MockHttpServletResponse response = new MockHttpServletResponse();

        page.render(Map.of(
                "query", "zzzz",
                "resultCount", 0,
                "results", List.of()
        ), request, response);

        String html = response.getContentAsString();
        assertTrue(html.contains("No courses matched your search"));
        assertTrue(html.contains("action=\"/search\""));
        assertTrue(html.contains("name=\"q\""));
        assertTrue(html.contains("Explore All Courses"));
        assertTrue(html.contains("href=\"/courses\""));
    }
}
