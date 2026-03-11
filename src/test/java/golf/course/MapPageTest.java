package golf.course;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapPageTest {

    @Test
    void renderHighlightsMapNavItemAndIncludesMapScripts() throws Exception {
        MapPage page = new MapPage();

        var request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setRequestURI("/map");
        var response = new org.springframework.mock.web.MockHttpServletResponse();

        List<MapPage.MapPoint> points = List.of(
                new MapPage.MapPoint("Ganton Golf Club", 54.231, -0.393, "/courses/ganton-golf-club")
        );

        page.render(Map.of(
                "mapPoints", points,
                "googleMapsApiKey", "test-key"
        ), request, response);

        String html = response.getContentAsString();

        assertTrue(
                html.contains("href=\"/map\" class=\"nav-link ygl-navbar__link ygl-navbar__link--active\"")
                        || html.contains("class=\"nav-link ygl-navbar__link ygl-navbar__link--active\" href=\"/map\"")
        );
        assertTrue(html.contains("id=\"course-map\""));
        assertTrue(html.contains("/js/map-page.js"));
        assertTrue(html.contains("maps.googleapis.com/maps/api/js"));
        assertTrue(html.contains("Ganton Golf Club"));
        assertTrue(html.contains("<a href=\"/map\">Map</a>"));
        assertTrue(html.contains("/courses/ganton-golf-club"));
    }

        @Test
        void renderWithoutApiKeyShowsFallbackEmbed() throws Exception {
        MapPage page = new MapPage();

        var request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setRequestURI("/map");
        var response = new org.springframework.mock.web.MockHttpServletResponse();

        page.render(Map.of(
                "mapPoints", List.of(new MapPage.MapPoint("Ganton Golf Club", 54.231, -0.393, "/courses/ganton-golf-club")),
                "googleMapsApiKey", ""
        ), request, response);

        String html = response.getContentAsString();
        assertTrue(html.contains("Google Maps API key is not configured"));
        assertTrue(html.contains("maps.google.com/maps?q="));
        assertTrue(html.contains("output=embed"));
        assertFalse(html.contains("maps.googleapis.com/maps/api/js"));
    }
}
