package golf.web;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyticsTest {

    @Test
    void sharedTemplateLoadsPostHogWithoutSimpleAnalytics() throws Exception {
        var response = new MockHttpServletResponse();
        new YorkshireGolfPageTemplate().withTitle("Test").render(response.getWriter());
        var page = Jsoup.parse(response.getContentAsString());

        assertEquals(0, page.select("script[src='https://scripts.simpleanalyticscdn.com/latest.js']").size());
        var snippets = page.select("head script").stream()
                .filter(script -> script.data().contains("posthog.init("))
                .toList();
        assertEquals(1, snippets.size());
        String snippet = snippets.get(0).data();
        assertTrue(snippet.contains("phc_z2WcTcL5FgWEDgnSsJ3W7jKuv9XZnypq7FJKwYGRBtnv"));
        assertTrue(snippet.contains("api_host: 'https://eu.i.posthog.com'"));
        assertTrue(snippet.contains("/static/array.js"));
        assertTrue(snippet.contains("p.async=!0"));
        assertTrue(snippet.replaceAll("\\s+", " ").contains("""
                posthog.init('phc_z2WcTcL5FgWEDgnSsJ3W7jKuv9XZnypq7FJKwYGRBtnv', {
                    api_host: 'https://eu.i.posthog.com',
                    defaults: '2026-05-30',
                    person_profiles: 'identified_only',
                    autocapture: false,
                    disable_session_recording: true,
                });
                """.strip().replaceAll("\\s+", " ")));
        assertFalse(snippet.contains("cookieless_mode"));
    }

    @Test
    void privacyPageDisclosesBothAnalyticsProvidersAndBrowserStorage() throws Exception {
        var response = new MockHttpServletResponse();
        new PrivacyPage().render(Map.of(), new MockHttpServletRequest(), response);
        String text = Jsoup.parse(response.getContentAsString()).text();

        assertFalse(text.contains("Simple Analytics"));
        assertTrue(text.contains("PostHog"));
        assertTrue(text.contains("EU cloud"));
        assertTrue(text.contains("uses cookies and browser storage"));
        assertFalse(text.contains("cookieless"));
        assertTrue(text.contains("Session recording and automatic click capture are disabled"));
    }
}
