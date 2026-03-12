///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.16.1
//DEPS com.fasterxml.jackson.core:jackson-databind:2.16.1
//DEPS org.jsoup:jsoup:1.18.1
//DEPS org.slf4j:slf4j-simple:2.0.9
//JAVA 21

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * YGUAudit - Cross-references YGU affiliate courses against our local course list.
 *
 * Run from repo root with: jbang scripts/YGUAudit.java
 *
 * Fetches the full list of affiliate courses from https://www.yugc.co.uk/course-rating/,
 * loads local course YAML files, normalises names for comparison, then prints a report
 * showing: exact matches, close matches, YGU-only courses (candidates to add), and
 * local-only courses (candidates to review / remove).
 */
public class YGUAudit {

    private static final String YGU_URL = "https://www.yugc.co.uk/course-rating/";
    private static final String COURSES_PATH = "src/main/resources/courses";
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 20_000;

    // Pre-compiled regex patterns used in hot paths
    private static final java.util.regex.Pattern PAR_WORD = java.util.regex.Pattern.compile("\\bpar\\b");
    private static final java.util.regex.Pattern PAREN_CONTENT = java.util.regex.Pattern.compile("\\s*\\(.*?\\)");
    private static final java.util.regex.Pattern COMMON_SUFFIXES = java.util.regex.Pattern.compile(
        "\\b(golf club|golf course|golf centre|golf and leisure|" +
        "golf park|golf range|golf academy|golf and country club|country club|" +
        "country house|hotel|ltd|limited)\\b");
    private static final java.util.regex.Pattern NON_ALNUM = java.util.regex.Pattern.compile("[^a-z0-9 ]");
    private static final java.util.regex.Pattern MULTI_SPACE = java.util.regex.Pattern.compile("\\s+");

    public static void main(String[] args) throws IOException {
        System.out.println("=== YGU Affiliate Course Audit ===");
        System.out.println();

        // 1. Fetch YGU courses
        System.out.println("Fetching YGU course list from " + YGU_URL + " ...");
        List<String> yguCourses = fetchYguCourses();
        System.out.println("  Found " + yguCourses.size() + " YGU affiliate courses");
        System.out.println();

        // 2. Load local courses
        System.out.println("Loading local course YAML files from " + COURSES_PATH + " ...");
        List<CourseInfo> localCourses = loadLocalCourses();
        System.out.println("  Found " + localCourses.size() + " local courses (" +
            localCourses.stream().filter(c -> !c.closed()).count() + " open, " +
            localCourses.stream().filter(CourseInfo::closed).count() + " closed)");
        System.out.println();

        // 3. Compare
        System.out.println("Comparing ...");
        ComparisonResult result = compare(yguCourses, localCourses);
        System.out.println();

        // 4. Print report
        printReport(result);
    }

    // -------------------------------------------------------------------------
    // YGU Fetching
    // -------------------------------------------------------------------------

    static List<String> fetchYguCourses() throws IOException {
        // Strategy 1: try with DataTables "show all" parameter (length=-1)
        String html = fetchHtml(YGU_URL + "?length=-1");
        if (html == null) {
            html = fetchHtml(YGU_URL);
        }
        if (html == null) {
            throw new IOException("Failed to fetch YGU course list from " + YGU_URL +
                "\nPlease check your internet connection and that the site is accessible.");
        }

        List<String> courses = parseCourseNames(html);

        // If we got very few courses from the first page, the list is paginated —
        // try common patterns to retrieve additional pages
        if (courses.size() < 50) {
            System.out.println("  (Initial page shows " + courses.size() + " courses — attempting to fetch all pages)");
            courses = fetchAllPages(html, courses);
        }

        // Deduplicate while preserving order
        Set<String> seen = new LinkedHashSet<>(courses);
        return new ArrayList<>(seen);
    }

    private static List<String> fetchAllPages(String firstPageHtml, List<String> firstPageCourses) {
        Document doc = Jsoup.parse(firstPageHtml);
        Set<String> allCourses = new LinkedHashSet<>(firstPageCourses);

        // Look for pagination links (standard WordPress / DataTables page links)
        Elements pageLinks = doc.select("a.page-numbers, .pagination a, .nav-links a, " +
            ".dataTables_paginate a, a[href*='page='], a[href*='/page/']");

        Set<String> visitedUrls = new HashSet<>();
        visitedUrls.add(YGU_URL);

        Set<String> pageUrls = new LinkedHashSet<>();
        for (Element link : pageLinks) {
            String href = link.absUrl("href");
            if (href.isEmpty()) {
                href = link.attr("href");
                if (!href.startsWith("http")) {
                    href = YGU_URL + href.replaceFirst("^/", "");
                }
            }
            if (!href.isEmpty() && !visitedUrls.contains(href)) {
                pageUrls.add(href);
            }
        }

        // Also try numeric pages (?paged=2, ?page=2, /page/2/)
        if (pageUrls.isEmpty()) {
            for (int page = 2; page <= 20; page++) {
                pageUrls.add(YGU_URL + "?paged=" + page);
                pageUrls.add(YGU_URL + "?page=" + page);
                pageUrls.add(YGU_URL + "page/" + page + "/");
            }
        }

        for (String pageUrl : pageUrls) {
            if (visitedUrls.contains(pageUrl)) continue;
            visitedUrls.add(pageUrl);

            String html = fetchHtml(pageUrl);
            if (html == null) continue;

            List<String> pageCourses = parseCourseNames(html);
            if (pageCourses.isEmpty()) continue;

            // Stop when we start seeing duplicates (all data on page was already seen)
            long newCount = pageCourses.stream().filter(c -> !allCourses.contains(c)).count();
            if (newCount == 0 && !allCourses.isEmpty()) break;

            allCourses.addAll(pageCourses);

            // Small delay to be polite
            try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        return new ArrayList<>(allCourses);
    }

    /**
     * Parses all golf course / club names from a page of the YGU course-rating table.
     * Tries multiple HTML structures that are commonly used on golf union websites.
     */
    static List<String> parseCourseNames(String html) {
        Document doc = Jsoup.parse(html, YGU_URL);
        List<String> names = new ArrayList<>();

        // Strategy A: DataTables / standard <table> — look for a table that contains
        // "Club" or "Course" in the header row, then extract the first column of each data row.
        for (Element table : doc.select("table")) {
            // Check if the table header looks like a course list
            String theadText = table.select("thead").text().toLowerCase();
            String firstRowText = table.select("tr").first() != null
                ? table.select("tr").first().text().toLowerCase() : "";

            if (theadText.contains("club") || theadText.contains("course") ||
                firstRowText.contains("club") || firstRowText.contains("course")) {

                // Collect first cell of each body row (skip header rows)
                for (Element row : table.select("tbody tr, tr")) {
                    Elements cells = row.select("td");
                    if (cells.isEmpty()) continue;
                    String text = cells.first().text().trim();
                    if (looksLikeCourseName(text)) {
                        names.add(text);
                    }
                }
                if (!names.isEmpty()) return names;

                // If first column didn't work, try all cells in the table
                for (Element cell : table.select("td")) {
                    String text = cell.text().trim();
                    if (looksLikeCourseName(text)) {
                        names.add(text);
                    }
                }
                if (!names.isEmpty()) return names;
            }
        }

        // Strategy B: any <table> on the page — extract cells that look like course names
        for (Element table : doc.select("table")) {
            for (Element cell : table.select("td")) {
                String text = cell.text().trim();
                if (looksLikeCourseName(text)) {
                    names.add(text);
                }
            }
            if (!names.isEmpty()) return names;
        }

        // Strategy C: list items or divs with golf-club-like text
        for (Element el : doc.select("li, .club-name, .course-name, [class*=club], [class*=course]")) {
            String text = el.text().trim();
            if (looksLikeCourseName(text)) {
                names.add(text);
            }
        }

        return names;
    }

    /**
     * Returns true if the text looks plausibly like a golf course / club name.
     * Filters out header row text, navigation labels, and other noise.
     */
    private static boolean looksLikeCourseName(String text) {
        if (text.isBlank()) return false;
        if (text.length() < 4 || text.length() > 100) return false;

        String lower = text.toLowerCase();

        // Must contain at least one golf-related keyword OR be a proper name with multiple words
        boolean hasGolfKeyword = lower.contains("golf") || lower.contains("park") ||
            lower.contains("hall") || lower.contains("manor") || lower.contains("links") ||
            lower.contains("course") || lower.contains("club");

        // Exclude obvious header / UI text
        if (lower.equals("club") || lower.equals("course") || lower.equals("name") ||
            lower.startsWith("show ") || lower.startsWith("search") ||
            lower.contains("entries") || lower.contains("filter") ||
            lower.contains("next") || lower.contains("previous") ||
            lower.contains("page") || lower.contains("rating") ||
            lower.contains("sss") || PAR_WORD.matcher(lower).find() ||
            lower.matches("[0-9]+")) {
            return false;
        }

        return hasGolfKeyword;
    }

    private static String fetchHtml(String urlStr) {
        try {
            URL url = URI.create(urlStr).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (compatible; YorkshireGolfBot/1.0; +https://yorkshiregolf.com)");
            conn.setInstanceFollowRedirects(true);

            int status = conn.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                return new String(conn.getInputStream().readAllBytes());
            }
            System.out.println("  HTTP " + status + " for " + urlStr);
        } catch (Exception e) {
            System.out.println("  Failed to fetch " + urlStr + ": " + e.getMessage());
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Local course loading
    // -------------------------------------------------------------------------

    static List<CourseInfo> loadLocalCourses() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Path coursesDir = Paths.get(COURSES_PATH);
        if (!Files.exists(coursesDir)) {
            throw new IOException("Courses directory not found: " + coursesDir.toAbsolutePath() +
                "\nPlease run from the repository root.");
        }

        List<CourseInfo> courses = new ArrayList<>();
        List<Path> files = Files.list(coursesDir)
            .filter(p -> p.toString().endsWith(".yaml"))
            .sorted()
            .collect(Collectors.toList());

        for (Path file : files) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = mapper.readValue(file.toFile(), Map.class);
                String name = (String) data.get("name");
                boolean closed = false;
                Object closedObj = data.get("closed");
                if (closedObj instanceof Boolean b) closed = b;
                else if (closedObj instanceof String s) closed = "true".equalsIgnoreCase(s);
                if (name != null && !name.isBlank()) {
                    courses.add(new CourseInfo(name.trim(), closed));
                }
            } catch (Exception e) {
                System.out.println("  Warning: could not parse " + file.getFileName() + ": " + e.getMessage());
            }
        }
        return courses;
    }

    // -------------------------------------------------------------------------
    // Comparison
    // -------------------------------------------------------------------------

    static ComparisonResult compare(List<String> yguCourses, List<CourseInfo> localCourses) {

        List<CourseInfo> openLocal = localCourses.stream().filter(c -> !c.closed()).toList();
        List<CourseInfo> closedLocal = localCourses.stream().filter(CourseInfo::closed).toList();

        List<MatchPair> exactMatches = new ArrayList<>();
        List<MatchPair> closeMatches = new ArrayList<>();
        List<String> yguOnly = new ArrayList<>();
        List<CourseInfo> localOnly = new ArrayList<>();
        List<MatchPair> yguMatchesClosed = new ArrayList<>();

        Set<String> matchedYgu = new HashSet<>();
        Set<String> matchedLocal = new HashSet<>();

        // Build normalised lookup for local open courses
        Map<String, CourseInfo> normalisedOpen = new LinkedHashMap<>();
        for (CourseInfo local : openLocal) {
            normalisedOpen.put(normalise(local.name()), local);
        }

        // Build normalised lookup for closed courses
        Map<String, CourseInfo> normalisedClosed = new LinkedHashMap<>();
        for (CourseInfo local : closedLocal) {
            normalisedClosed.put(normalise(local.name()), local);
        }

        // First pass: exact normalised matches against OPEN local courses
        for (String ygu : yguCourses) {
            String yguNorm = normalise(ygu);
            if (normalisedOpen.containsKey(yguNorm)) {
                CourseInfo local = normalisedOpen.get(yguNorm);
                exactMatches.add(new MatchPair(ygu, local.name()));
                matchedYgu.add(ygu);
                matchedLocal.add(local.name());
            }
        }

        // Second pass: close matches (token overlap) against OPEN local courses
        for (String ygu : yguCourses) {
            if (matchedYgu.contains(ygu)) continue;
            String yguNorm = normalise(ygu);
            Set<String> yguTokens = tokenise(yguNorm);

            CourseInfo bestLocal = null;
            double bestScore = 0.0;

            for (CourseInfo local : openLocal) {
                if (matchedLocal.contains(local.name())) continue;
                String localNorm = normalise(local.name());
                Set<String> localTokens = tokenise(localNorm);

                double score = jaccardSimilarity(yguTokens, localTokens);
                if (score > bestScore) {
                    bestScore = score;
                    bestLocal = local;
                }
            }

            if (bestLocal != null && bestScore >= 0.5) {
                closeMatches.add(new MatchPair(ygu, bestLocal.name(), bestScore));
                matchedYgu.add(ygu);
                matchedLocal.add(bestLocal.name());
            }
        }

        // Third pass: check unmatched YGU courses against CLOSED local courses
        for (String ygu : yguCourses) {
            if (matchedYgu.contains(ygu)) continue;
            String yguNorm = normalise(ygu);

            // Exact match against closed
            if (normalisedClosed.containsKey(yguNorm)) {
                CourseInfo closed = normalisedClosed.get(yguNorm);
                yguMatchesClosed.add(new MatchPair(ygu, closed.name()));
                matchedYgu.add(ygu);
                continue;
            }

            // Close match against closed (token overlap, higher threshold)
            Set<String> yguTokens = tokenise(yguNorm);
            for (CourseInfo closed : closedLocal) {
                String closedNorm = normalise(closed.name());
                Set<String> closedTokens = tokenise(closedNorm);
                double score = jaccardSimilarity(yguTokens, closedTokens);
                if (score >= 0.7) {
                    yguMatchesClosed.add(new MatchPair(ygu, closed.name(), score));
                    matchedYgu.add(ygu);
                    break;
                }
            }
        }

        // YGU-only (not matched to any local course, open or closed)
        for (String ygu : yguCourses) {
            if (!matchedYgu.contains(ygu)) {
                yguOnly.add(ygu);
            }
        }

        // Local-only (open courses not matched to any YGU course)
        for (CourseInfo local : openLocal) {
            if (!matchedLocal.contains(local.name())) {
                localOnly.add(local);
            }
        }

        return new ComparisonResult(exactMatches, closeMatches, yguOnly, localOnly,
            yguMatchesClosed, closedLocal);
    }

    /**
     * Normalises a course name for comparison by stripping common suffixes,
     * punctuation, and whitespace.
     */
    static String normalise(String name) {
        String n = name.toLowerCase(Locale.ENGLISH);

        // Remove parenthesised content like "(The Limes)" or "(closed)"
        n = PAREN_CONTENT.matcher(n).replaceAll("");

        // Normalise ampersand first so patterns like "golf & country club" can be matched
        n = n.replace("&", "and");

        // Remove common trailing words that vary between lists
        n = COMMON_SUFFIXES.matcher(n).replaceAll(" ");

        // Remove punctuation and normalise whitespace
        n = NON_ALNUM.matcher(n).replaceAll(" ");
        n = MULTI_SPACE.matcher(n).replaceAll(" ").trim();

        return n;
    }

    private static Set<String> tokenise(String normalised) {
        // Filter short words (stop words)
        return Arrays.stream(normalised.split(" "))
            .filter(t -> t.length() > 2)
            .collect(Collectors.toSet());
    }

    private static double jaccardSimilarity(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 1.0;
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) intersection.size() / union.size();
    }

    // -------------------------------------------------------------------------
    // Report
    // -------------------------------------------------------------------------

    private static void printReport(ComparisonResult result) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║              YGU Affiliate Course Audit Report               ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.printf("  Exact matches        : %d%n", result.exactMatches().size());
        System.out.printf("  Close matches        : %d  ← possible name discrepancies%n", result.closeMatches().size());
        System.out.printf("  YGU only             : %d  ← may need to be ADDED to our list%n", result.yguOnly().size());
        System.out.printf("  YGU matches CLOSED   : %d  ← YGU affiliate but we have CLOSED — review status%n", result.yguMatchesClosed().size());
        System.out.printf("  Local only (open)    : %d  ← may need to be REVIEWED / REMOVED%n", result.localOnly().size());
        System.out.printf("  Local closed         : %d%n", result.closedLocal().size());
        System.out.println();

        // Exact matches
        System.out.println("── EXACT MATCHES (" + result.exactMatches().size() + ") ─────────────────────────────────────────");
        for (MatchPair m : result.exactMatches()) {
            if (m.yguName().equalsIgnoreCase(m.localName())) {
                System.out.println("  ✔  " + m.yguName());
            } else {
                System.out.printf("  ✔  %-50s  ↔  %s%n", m.yguName(), m.localName());
            }
        }
        System.out.println();

        // Close matches (possible name discrepancies)
        System.out.println("── CLOSE MATCHES — possible name discrepancies (" + result.closeMatches().size() + ") ─────");
        if (result.closeMatches().isEmpty()) {
            System.out.println("  (none)");
        }
        for (MatchPair m : result.closeMatches()) {
            System.out.printf("  ~  YGU:   %s%n", m.yguName());
            System.out.printf("     Local: %s  (score: %.0f%%)%n", m.localName(), m.score() * 100);
        }
        System.out.println();

        // YGU only — candidates to add
        System.out.println("── YGU ONLY — candidates to ADD to our list (" + result.yguOnly().size() + ") ────────────");
        if (result.yguOnly().isEmpty()) {
            System.out.println("  (none)");
        }
        for (String name : result.yguOnly()) {
            System.out.println("  +  " + name);
        }
        System.out.println();

        // YGU courses that match our closed list — worth reviewing status
        System.out.println("── YGU AFFILIATE but CLOSED in our list — review status (" + result.yguMatchesClosed().size() + ") ──");
        if (result.yguMatchesClosed().isEmpty()) {
            System.out.println("  (none)");
        }
        for (MatchPair m : result.yguMatchesClosed()) {
            if (m.score() < 1.0) {
                System.out.printf("  ?  YGU:    %s%n", m.yguName());
                System.out.printf("     Closed: %s  (score: %.0f%%)%n", m.localName(), m.score() * 100);
            } else {
                System.out.printf("  ?  %-50s  ↔  %s (CLOSED)%n", m.yguName(), m.localName());
            }
        }
        System.out.println();

        // Local-only (open) — candidates to review / remove
        System.out.println("── LOCAL ONLY (open) — candidates to REVIEW / REMOVE (" + result.localOnly().size() + ") ──");
        if (result.localOnly().isEmpty()) {
            System.out.println("  (none)");
        }
        for (CourseInfo course : result.localOnly()) {
            System.out.println("  -  " + course.name());
        }
        System.out.println();

        // Closed local courses (for reference)
        System.out.println("── LOCAL CLOSED courses (" + result.closedLocal().size() + ") ───────────────────────────────────");
        for (CourseInfo course : result.closedLocal()) {
            System.out.println("  ✖  " + course.name());
        }
        System.out.println();
    }

    // -------------------------------------------------------------------------
    // Data types
    // -------------------------------------------------------------------------

    record CourseInfo(String name, boolean closed) {}

    record MatchPair(String yguName, String localName, double score) {
        MatchPair(String yguName, String localName) { this(yguName, localName, 1.0); }
    }

    record ComparisonResult(
        List<MatchPair> exactMatches,
        List<MatchPair> closeMatches,
        List<String> yguOnly,
        List<CourseInfo> localOnly,
        List<MatchPair> yguMatchesClosed,
        List<CourseInfo> closedLocal
    ) {}
}
