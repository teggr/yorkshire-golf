package golf.course;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GolfNowDirectoryParserTest {

    @Test
    void extractsCountyUrlsFromEnglandDirectory() throws Exception {
        String html = readFixture("golfnow/eng-sample.html");

        Map<String, String> counties = GolfNowDirectoryParser.extractCountyUrls(
                html,
                List.of("Yorkshire", "Lancashire", "Derbyshire")
        );

        assertEquals("https://www.golfnow.co.uk/course-directory/eng/yo", counties.get("Yorkshire"));
        assertEquals("https://www.golfnow.co.uk/course-directory/eng/la", counties.get("Lancashire"));
        assertEquals("https://www.golfnow.co.uk/course-directory/eng/de", counties.get("Derbyshire"));
    }

    @Test
    void extractsSubregionsFromCountyPage() throws Exception {
        String html = readFixture("golfnow/county-yo-sample.html");

        List<GolfNowDirectoryParser.Subregion> subregions = GolfNowDirectoryParser.extractSubregions("Yorkshire", html);

        assertEquals(2, subregions.size());
        assertEquals("Leeds", subregions.get(0).subregion());
        assertEquals("https://www.golfnow.co.uk/course-directory/eng/yo/21687-leeds", subregions.get(0).url());
        assertEquals("York", subregions.get(1).subregion());
        assertEquals("https://www.golfnow.co.uk/course-directory/eng/yo/21453-york", subregions.get(1).url());
    }

    @Test
    void extractsCoursesAndCleansNames() throws Exception {
        String html = readFixture("golfnow/subregion-leeds-sample.html");
        var subregion = new GolfNowDirectoryParser.Subregion("Yorkshire", "Leeds", "https://www.golfnow.co.uk/course-directory/eng/yo/21687-leeds");

        List<GolfNowDirectoryParser.CourseRow> courses = GolfNowDirectoryParser.extractCourses(subregion, html);

        assertEquals(2, courses.size());
        assertEquals("Moor Allerton Golf Club - Blackmoor", courses.get(0).courseName());
        assertEquals("South Bradford Golf Club", courses.get(1).courseName());
        assertEquals("https://www.golfnow.co.uk/courses/-5000-south-bradford-golf-club-details", courses.get(1).courseUrl());
    }

    @Test
    void extractsTeeTimesFacilityUrlFromCourseHtml() throws Exception {
        String html = readFixture("golfnow/course-detail-sample.html");

        String teeTimesUrl = GolfNowDirectoryParser.extractTeeTimesUrlFromCourseHtml(
                "https://www.golfnow.co.uk/courses/-5000-south-bradford-golf-club-details",
                html
        );

        assertEquals("https://www.golfnow.co.uk/tee-times/facility/16120-south-bradford-golf-club/search", teeTimesUrl);
    }

    @Test
    void extractsNearbyTeeTimesUrlFromCourseHtml() throws Exception {
        String html = readFixture("golfnow/course-detail-sample.html");

        String nearbyTeeTimesUrl = GolfNowDirectoryParser.extractNearbyTeeTimesUrlFromCourseHtml(
                "https://www.golfnow.co.uk/courses/-5000-south-bradford-golf-club-details",
                html
        );

        assertEquals("https://www.golfnow.co.uk/tee-times/location/lat/53.869873/lng/-1.48334/place/leeds-yo-eng", nearbyTeeTimesUrl);
    }

    @Test
    void returnsBlankWhenNoFacilityPatternPresent() {
        String teeTimesUrl = GolfNowDirectoryParser.extractTeeTimesUrlFromCourseHtml(
                "https://www.golfnow.co.uk/courses/-5000-south-bradford-golf-club-details",
                "<html><body>No facility pattern</body></html>"
        );

        assertEquals("", teeTimesUrl);
    }

    @Test
    void ignoresPlaceholderFacilityZero() {
        String teeTimesUrl = GolfNowDirectoryParser.extractTeeTimesUrlFromCourseHtml(
                "https://www.golfnow.co.uk/courses/-5000-south-bradford-golf-club-details",
                "<html><body><img src=\"https://exddilid.cdn.imgeng.in/app/ttf/image/bh/0.jpg\" /></body></html>"
        );

        assertEquals("", teeTimesUrl);
    }

    @Test
    void extractsFacilityIdsFromScriptDrivenSubregionHtml() {
        String html = """
                <html><body>
                <script>
                    let redirectToFacilityLink = '/tee-times/facility/~facilityid~/search';
                    const reviewIds = [11128,15021];
                </script>
                <courses-by-ids :course-ids="[11128,15021]"></courses-by-ids>
                </body></html>
                """;

        List<String> ids = GolfNowDirectoryParser.extractFacilityIdsFromSubregionHtml(html);

        assertEquals(List.of("11128", "15021"), ids);
        assertEquals("/tee-times/facility/~facilityid~/search", GolfNowDirectoryParser.extractFacilityLinkTemplate(html));
    }

    @Test
    void extractsCourseInfoFromFacilityPageHtml() {
        String html = """
                <html><head>
                <title>Baildon Golf Club Tee Times - Shipley, Yorkshire</title>
                <link rel="canonical" href="https://www.golfnow.co.uk/tee-times/facility/11128-baildon-golf-club/search" />
                </head><body>
                <tee-time-booking course-info-url="/courses/-2142-baildon-golf-club-details"></tee-time-booking>
                </body></html>
                """;

        GolfNowDirectoryParser.FacilityCourseInfo info = GolfNowDirectoryParser.extractFacilityCourseInfoFromHtml(
                "https://www.golfnow.co.uk/tee-times/facility/11128/search",
                html
        );

        assertEquals("Baildon Golf Club", info.courseName());
        assertEquals("https://www.golfnow.co.uk/courses/-2142-baildon-golf-club-details", info.courseUrl());
        assertEquals("https://www.golfnow.co.uk/tee-times/facility/11128-baildon-golf-club/search", info.teeTimesUrl());
    }

    private static String readFixture(String resourcePath) throws IOException {
        var stream = GolfNowDirectoryParserTest.class.getClassLoader().getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new IOException("Fixture not found: " + resourcePath);
        }
        return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
}
