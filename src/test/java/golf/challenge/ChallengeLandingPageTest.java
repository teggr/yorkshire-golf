package golf.challenge;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChallengeLandingPageTest {

    @Test
    void renderShowsChallengeIntroSignupAndTrackerImage() throws Exception {
        ChallengeLandingPage page = new ChallengeLandingPage();

        var request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setRequestURI("/challenge");
        var response = new org.springframework.mock.web.MockHttpServletResponse();

        page.render(Map.of("totalCourseCount", 194), request, response);

        String html = response.getContentAsString();
        String contentType = response.getContentType();

        assertNotNull(contentType);
        assertTrue(contentType.startsWith("text/html"));

        assertTrue(html.contains("Challenge Tracker"));
        assertTrue(html.contains("194"));
        assertTrue(html.contains("four regions"));
        assertTrue(html.contains("By signing up to Yorkshire Golf Life"));
        assertTrue(html.contains("register your rounds"));
        assertTrue(html.contains("/register"));
        assertTrue(html.contains("/images/challenge/challenge-tracker-landing.svg"));
        assertTrue(
            html.contains("href=\"/challenge\" class=\"nav-link ygl-navbar__link ygl-navbar__link--active\"")
                || html.contains("class=\"nav-link ygl-navbar__link ygl-navbar__link--active\" href=\"/challenge\"")
        );
        assertTrue(
            html.contains("href=\"/challenge\" class=\"nav-link ygl-navbar__link ygl-navbar__link--active\" aria-current=\"page\"")
                || html.contains("class=\"nav-link ygl-navbar__link ygl-navbar__link--active\" href=\"/challenge\" aria-current=\"page\"")
                || html.contains("class=\"nav-link ygl-navbar__link ygl-navbar__link--active\" aria-current=\"page\" href=\"/challenge\"")
        );
        assertFalse(
            html.contains("href=\"/courses\" class=\"nav-link ygl-navbar__link ygl-navbar__link--active\"")
                || html.contains("class=\"nav-link ygl-navbar__link ygl-navbar__link--active\" href=\"/courses\"")
        );
    }
}