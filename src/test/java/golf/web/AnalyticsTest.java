package golf.web;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyticsTest {

    @Test
    void sharedTemplateLoadsPostHogAlongsideSimpleAnalytics() throws Exception {
        var response = new MockHttpServletResponse();
        new YorkshireGolfPageTemplate().withTitle("Test").render(response.getWriter());
        var page = Jsoup.parse(response.getContentAsString());

        assertEquals(1, page.select("script[src='https://scripts.simpleanalyticscdn.com/latest.js'][async]").size());
        var snippets = page.select("head script").stream()
                .filter(script -> script.data().contains("posthog.init("))
                .toList();
        assertEquals(1, snippets.size());
        String snippet = snippets.get(0).data();
        assertTrue(snippet.contains("phc_z2WcTcL5FgWEDgnSsJ3W7jKuv9XZnypq7FJKwYGRBtnv"));
        assertTrue(snippet.contains("api_host: 'https://eu.i.posthog.com'"));
        assertTrue(snippet.contains("/static/array.js"));
        assertTrue(snippet.contains("p.async=!0"));
        assertTrue(snippet.contains("defaults: '2026-08-30'"));
        assertTrue(snippet.contains("cookieless_mode: 'always'"));
        assertTrue(snippet.contains("person_profiles: 'never'"));
        assertTrue(snippet.contains("autocapture: false"));
        assertTrue(snippet.contains("capture_pageview: true"));
        assertTrue(snippet.contains("capture_pageleave: false"));
        assertTrue(snippet.contains("capture_heatmaps: false"));
        assertTrue(snippet.contains("capture_performance: false"));
        assertTrue(snippet.contains("disable_session_recording: true"));
        assertTrue(snippet.contains("disable_surveys: true"));
    }

    @Test
    void privacyPageDisclosesBothAnalyticsProvidersAndCookielessSettings() throws Exception {
        var response = new MockHttpServletResponse();
        new PrivacyPage().render(Map.of(), new MockHttpServletRequest(), response);
        String text = Jsoup.parse(response.getContentAsString()).text();

        assertTrue(text.contains("Simple Analytics"));
        assertTrue(text.contains("PostHog"));
        assertTrue(text.contains("EU cloud"));
        assertTrue(text.contains("without analytics cookies or browser storage"));
        assertTrue(text.contains("Session recording and automatic click capture are disabled"));
    }
}
