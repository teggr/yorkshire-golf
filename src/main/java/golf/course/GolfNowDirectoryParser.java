package golf.course;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GolfNowDirectoryParser {

    private static final String ENGLAND_DIRECTORY_URL = "https://www.golfnow.co.uk/course-directory/eng";
    private static final String BASE_URL = "https://www.golfnow.co.uk";
    private static final Pattern FACILITY_ID_PATTERN = Pattern.compile("(?:/tee-times/facility/|/app/ttf/image/bh/)(\\d+)");
    private static final Pattern COURSE_SLUG_PATTERN = Pattern.compile(".*/courses/-?\\d+-([a-z0-9-]+)-details/?$");
    private static final Pattern COURSE_IDS_COMPONENT_PATTERN = Pattern.compile(":course-ids=\\\"\\[([^\\]]+)\\]\\\"");
    private static final Pattern REVIEW_IDS_PATTERN = Pattern.compile("reviewIds\\s*=\\s*\\[([^\\]]+)\\]");
    private static final Pattern REDIRECT_FACILITY_LINK_PATTERN = Pattern.compile("redirectToFacilityLink\\s*=\\s*'([^']+~facilityid~[^']*)'");

    private GolfNowDirectoryParser() {
    }

    public record Subregion(String region, String subregion, String url) {
    }

    public record CourseRow(String region, String subregion, String courseName, String courseUrl, String teeTimesUrl) {
    }

    public record FacilityCourseInfo(String courseName, String courseUrl, String teeTimesUrl) {
    }

    public static Map<String, String> extractCountyUrls(String englandHtml, List<String> targetCounties) {
        Document doc = Jsoup.parse(englandHtml, ENGLAND_DIRECTORY_URL);
        Map<String, String> result = new HashMap<>();

        Elements countyLinks = doc.select("a[href^=/course-directory/eng/], a[href^=https://www.golfnow.co.uk/course-directory/eng/]");
        Set<String> targets = new LinkedHashSet<>();
        for (String county : targetCounties) {
            targets.add(normalizeCountyName(county));
        }

        for (Element link : countyLinks) {
            String name = link.text().trim();
            if (name.isEmpty()) {
                continue;
            }
            String normalized = normalizeCountyName(name);
            if (!targets.contains(normalized)) {
                continue;
            }

            String absUrl = absoluteUrl(link.attr("href"));
            if (absUrl.matches("https://www\\.golfnow\\.co\\.uk/course-directory/eng/[a-z]{2}/?")) {
                String key = findOriginalCountyName(targetCounties, normalized);
                result.putIfAbsent(key, canonicalUrl(absUrl));
            }
        }

        return result;
    }

    public static List<Subregion> extractSubregions(String region, String countyHtml) {
        Document doc = Jsoup.parse(countyHtml);
        Elements links = doc.select("a[href*=/course-directory/eng/]");

        List<Subregion> subregions = new ArrayList<>();
        Set<String> seenUrls = new LinkedHashSet<>();

        for (Element link : links) {
            String href = canonicalUrl(absoluteUrl(link.attr("href")));
            if (!href.matches("https://www\\.golfnow\\.co\\.uk/course-directory/eng/[a-z]{2}/[0-9]+-[a-z0-9-]+/?")) {
                continue;
            }

            if (!seenUrls.add(href)) {
                continue;
            }

            String text = link.text().trim();
            if (text.isBlank()) {
                continue;
            }

            String subregion = text.replaceFirst("(?i)\\s+golf\\s+courses$", "").trim();
            if (subregion.isBlank()) {
                continue;
            }

            subregions.add(new Subregion(region, subregion, href));
        }

        return subregions;
    }

    public static List<CourseRow> extractCourses(Subregion subregion, String subregionHtml) {
        Document doc = Jsoup.parse(subregionHtml);
        Elements courseLinks = doc.select("a[href*=/courses/][href$=-details], a[href*=/courses/][href*=-details]");

        List<CourseRow> rows = new ArrayList<>();
        Set<String> seenCourseUrls = new LinkedHashSet<>();

        for (Element link : courseLinks) {
            String courseUrl = absoluteUrl(link.attr("href"));
            if (!courseUrl.contains("/courses/")) {
                continue;
            }
            if (!seenCourseUrls.add(courseUrl)) {
                continue;
            }

            String courseName = extractCourseName(link, courseUrl);
            rows.add(new CourseRow(
                    subregion.region(),
                    subregion.subregion(),
                    courseName,
                    courseUrl,
                    ""
            ));
        }

        return rows;
    }

    public static List<String> extractFacilityIdsFromSubregionHtml(String subregionHtml) {
        if (subregionHtml == null || subregionHtml.isBlank()) {
            return List.of();
        }

        Set<String> ids = new LinkedHashSet<>();
        collectIdsFromPattern(ids, COURSE_IDS_COMPONENT_PATTERN.matcher(subregionHtml));
        collectIdsFromPattern(ids, REVIEW_IDS_PATTERN.matcher(subregionHtml));
        return new ArrayList<>(ids);
    }

    public static String extractFacilityLinkTemplate(String subregionHtml) {
        if (subregionHtml == null || subregionHtml.isBlank()) {
            return "/tee-times/facility/~facilityid~/search";
        }

        Matcher matcher = REDIRECT_FACILITY_LINK_PATTERN.matcher(subregionHtml);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return "/tee-times/facility/~facilityid~/search";
    }

    public static FacilityCourseInfo extractFacilityCourseInfoFromHtml(String facilityUrl, String facilityHtml) {
        if (facilityHtml == null || facilityHtml.isBlank()) {
            return new FacilityCourseInfo("", "", "");
        }

        Document doc = Jsoup.parse(facilityHtml, facilityUrl);

        String teeTimesUrl = "";
        Element canonical = doc.selectFirst("link[rel=canonical][href]");
        if (canonical != null) {
            teeTimesUrl = absoluteUrl(canonical.attr("href"));
        }
        if (teeTimesUrl.isBlank()) {
            teeTimesUrl = facilityUrl == null ? "" : facilityUrl;
        }

        String courseUrl = "";
        Element courseInfoElement = doc.selectFirst("[course-info-url]");
        if (courseInfoElement != null) {
            courseUrl = absoluteUrl(courseInfoElement.attr("course-info-url"));
        }

        if (courseUrl.isBlank()) {
            Element reviewElement = doc.selectFirst("[read-review-page-url]");
            if (reviewElement != null) {
                String readReviewUrl = reviewElement.attr("read-review-page-url");
                int hashIndex = readReviewUrl.indexOf('#');
                if (hashIndex >= 0) {
                    readReviewUrl = readReviewUrl.substring(0, hashIndex);
                }
                courseUrl = absoluteUrl(readReviewUrl);
            }
        }

        if (courseUrl.isBlank()) {
            Element courseLink = doc.selectFirst("a[href*=/courses/][href*=-details]");
            if (courseLink != null) {
                courseUrl = absoluteUrl(courseLink.attr("href"));
            }
        }

        String courseName = "";
        if (!courseUrl.isBlank()) {
            courseName = titleCaseSlug(courseUrl);
        }

        if (courseName.isBlank() || "Unknown Course".equals(courseName)) {
            Element titleElement = doc.selectFirst("title");
            if (titleElement != null) {
                String title = titleElement.text().trim();
                courseName = title.replaceFirst("(?i)\\s+tee\\s+times\\s*[-|:].*$", "").trim();
            }
        }

        return new FacilityCourseInfo(courseName, courseUrl, teeTimesUrl);
    }

    public static String extractTeeTimesUrlFromCourseHtml(String courseUrl, String courseHtml) {
        if (courseHtml == null || courseHtml.isBlank()) {
            return "";
        }

        Document doc = Jsoup.parse(courseHtml, courseUrl);
        Elements links = doc.select("a[href*=/tee-times/facility/]");
        for (Element link : links) {
            String href = absoluteUrl(link.attr("href"));
            Matcher matcher = FACILITY_ID_PATTERN.matcher(href);
            if (matcher.find()) {
                String facilityId = matcher.group(1);
                if (isUsableFacilityId(facilityId)) {
                    return buildFacilityUrl(facilityId, courseUrl);
                }
            }
        }

        Matcher htmlMatcher = FACILITY_ID_PATTERN.matcher(courseHtml);
        while (htmlMatcher.find()) {
            String facilityId = htmlMatcher.group(1);
            if (isUsableFacilityId(facilityId)) {
                return buildFacilityUrl(facilityId, courseUrl);
            }
        }

        return "";
    }

    public static String extractNearbyTeeTimesUrlFromCourseHtml(String courseUrl, String courseHtml) {
        if (courseHtml == null || courseHtml.isBlank()) {
            return "";
        }

        Document doc = Jsoup.parse(courseHtml, courseUrl);
        Element nearbyLink = doc.selectFirst("gn-link[href*=/tee-times/location/], a[href*=/tee-times/location/]");
        if (nearbyLink == null) {
            return "";
        }

        return absoluteUrl(nearbyLink.attr("href"));
    }

    private static boolean isUsableFacilityId(String facilityId) {
        return facilityId != null && !facilityId.isBlank() && !"0".equals(facilityId);
    }

    private static void collectIdsFromPattern(Set<String> ids, Matcher matcher) {
        while (matcher.find()) {
            String rawList = matcher.group(1);
            if (rawList == null || rawList.isBlank()) {
                continue;
            }

            String[] parts = rawList.split(",");
            for (String part : parts) {
                String candidate = part.replaceAll("[^0-9]", "").trim();
                if (isUsableFacilityId(candidate)) {
                    ids.add(candidate);
                }
            }
        }
    }

    public static String toCsv(List<CourseRow> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("region,subregion,course_name,course_url,tee_times_url\n");

        for (CourseRow row : rows) {
            sb.append(csvCell(row.region())).append(',')
                    .append(csvCell(row.subregion())).append(',')
                    .append(csvCell(row.courseName())).append(',')
                    .append(csvCell(row.courseUrl())).append(',')
                    .append(csvCell(row.teeTimesUrl()))
                    .append('\n');
        }

        return sb.toString();
    }

    private static String extractCourseName(Element link, String courseUrl) {
        String title = link.attr("title").trim();
        if (title.startsWith("View ") && title.endsWith(" Details") && title.length() > "View  Details".length()) {
            return title.substring(5, title.length() - 8).trim();
        }

        String fromSlug = titleCaseSlug(courseUrl);
        if (!fromSlug.isBlank() && !"Unknown Course".equals(fromSlug)) {
            return fromSlug;
        }

        String text = link.text().replaceAll("\\s+", " ").trim();
        if (!text.isBlank()) {
            String cleaned = text.replaceFirst("(?i)\\s+view\\s+course$", "").trim();
            cleaned = cleaned.replaceAll("\\s+[A-Z]{1,2}\\d{1,2}[A-Z]?\\s*\\d[A-Z]{2}$", "").trim();
            if (!cleaned.isBlank()) {
                return cleaned;
            }
        }

        return titleCaseSlug(courseUrl);
    }

    private static String titleCaseSlug(String courseUrl) {
        String slug = courseUrl.replaceFirst(".*/courses/", "");
        slug = slug.replaceFirst("^-?\\d+-", "");
        slug = slug.replaceFirst("-details/?$", "");
        slug = slug.replace('-', ' ').trim();
        if (slug.isBlank()) {
            return "Unknown Course";
        }

        String[] parts = slug.split(" ");
        StringBuilder titleCase = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!titleCase.isEmpty()) {
                titleCase.append(' ');
            }
            if (part.length() == 1) {
                titleCase.append(part.toUpperCase(Locale.ROOT));
            } else {
                titleCase.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
                titleCase.append(part.substring(1));
            }
        }

        return titleCase.toString();
    }

    private static String buildFacilityUrl(String facilityId, String courseUrl) {
        String slug = extractSlugFromCourseUrl(courseUrl);
        if (slug.isBlank()) {
            return BASE_URL + "/tee-times/facility/" + facilityId + "/search";
        }
        return BASE_URL + "/tee-times/facility/" + facilityId + "-" + slug + "/search";
    }

    private static String extractSlugFromCourseUrl(String courseUrl) {
        Matcher slugMatcher = COURSE_SLUG_PATTERN.matcher(courseUrl);
        if (slugMatcher.matches()) {
            return slugMatcher.group(1);
        }
        return "";
    }

    private static String normalizeCountyName(String county) {
        return county.replaceAll("\\s+", " ")
                .replaceAll("\\([0-9]+\\)", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private static String findOriginalCountyName(List<String> originalNames, String normalized) {
        for (String original : originalNames) {
            if (normalizeCountyName(original).equals(normalized)) {
                return original;
            }
        }
        return normalized;
    }

    private static String csvCell(String value) {
        String safe = value == null ? "" : value;
        String escaped = safe.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private static String absoluteUrl(String href) {
        if (href == null || href.isBlank()) {
            return "";
        }
        if (href.startsWith("http://") || href.startsWith("https://")) {
            return href;
        }
        if (href.startsWith("//")) {
            return "https:" + href;
        }
        if (!href.startsWith("/")) {
            return BASE_URL + "/" + href;
        }
        return BASE_URL + href;
    }

    private static String canonicalUrl(String url) {
        if (url == null) {
            return "";
        }
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
