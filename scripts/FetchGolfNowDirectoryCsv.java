///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.jsoup:jsoup:1.17.2
//SOURCES src/main/java/golf/course/GolfNowDirectoryParser.java
//JAVA 21

import golf.course.GolfNowDirectoryParser;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Export GolfNow directory data to CSV for Yorkshire and neighboring counties.
 *
 * Run from repo root:
 *   jbang scripts/FetchGolfNowDirectoryCsv.java
 *
 * Useful options:
 *   jbang scripts/FetchGolfNowDirectoryCsv.java --dry-run --limit-subregions 5
 *   jbang scripts/FetchGolfNowDirectoryCsv.java --output data/golfnow-courses.csv
 */
public class FetchGolfNowDirectoryCsv {

    private static final String ENGLAND_DIRECTORY_URL = "https://www.golfnow.co.uk/course-directory/eng";
    private static final String DEFAULT_OUTPUT = "data/golfnow-courses.csv";

    private static final List<String> TARGET_COUNTIES = List.of(
            "Yorkshire",
            "Lancashire",
            "Derbyshire",
            "Lincolnshire",
            "Nottinghamshire",
            "Durham",
            "Northumberland"
    );

    private static final String USER_AGENT = "Mozilla/5.0 (compatible; YorkshireGolfFetcher/1.0)";
    private static final int HTTP_TIMEOUT_MS = 20000;
    private static final String TEE_TIMES_SOURCE_NONE = "none";
    private static final String TEE_TIMES_SOURCE_NEARBY = "nearby";
    private static final String TEE_TIMES_SOURCE_FACILITY = "facility";

    record Config(boolean dryRun, String outputPath, int limitSubregions, int delayMs, int maxRetries, String teeTimesSource) {
    }

    public static void main(String[] args) throws Exception {
        Config config = parseArgs(args);

        System.out.println("Target counties: " + String.join(", ", TARGET_COUNTIES));
        System.out.println("Dry run: " + config.dryRun());
        System.out.println("Output: " + config.outputPath());
        System.out.println("Tee times source: " + config.teeTimesSource());

        String englandHtml = fetchWithRetry(ENGLAND_DIRECTORY_URL, config.maxRetries());
        if (englandHtml == null) {
            throw new IllegalStateException("Unable to fetch England directory: " + ENGLAND_DIRECTORY_URL);
        }

        Map<String, String> countyUrls = GolfNowDirectoryParser.extractCountyUrls(englandHtml, TARGET_COUNTIES);
        for (String county : TARGET_COUNTIES) {
            if (!countyUrls.containsKey(county)) {
                System.err.println("WARNING: County not found on England page: " + county);
            }
        }

        List<GolfNowDirectoryParser.Subregion> subregions = new ArrayList<>();
        for (String county : TARGET_COUNTIES) {
            String countyUrl = countyUrls.get(county);
            if (countyUrl == null) {
                continue;
            }

            String countyHtml = fetchWithRetry(countyUrl, config.maxRetries());
            if (countyHtml == null) {
                System.err.println("WARNING: Failed county page: " + countyUrl);
                continue;
            }

            List<GolfNowDirectoryParser.Subregion> found = GolfNowDirectoryParser.extractSubregions(county, countyHtml);
            System.out.printf("[%s] found %d subregions%n", county, found.size());
            subregions.addAll(found);
            sleep(config.delayMs());
        }

        Map<String, GolfNowDirectoryParser.Subregion> uniqueSubregionsByUrl = new LinkedHashMap<>();
        for (GolfNowDirectoryParser.Subregion subregion : subregions) {
            uniqueSubregionsByUrl.putIfAbsent(canonicalUrl(subregion.url()), subregion);
        }
        List<GolfNowDirectoryParser.Subregion> uniqueSubregions = new ArrayList<>(uniqueSubregionsByUrl.values());

        if (config.limitSubregions() > 0 && uniqueSubregions.size() > config.limitSubregions()) {
            uniqueSubregions = uniqueSubregions.subList(0, config.limitSubregions());
        }

        List<GolfNowDirectoryParser.CourseRow> rows = new ArrayList<>();
        int index = 0;
        for (GolfNowDirectoryParser.Subregion subregion : uniqueSubregions) {
            index++;
            System.out.printf("[%d/%d] Scraping %s / %s%n", index, uniqueSubregions.size(), subregion.region(), subregion.subregion());

            String subregionHtml = fetchWithRetry(subregion.url(), config.maxRetries());
            if (subregionHtml == null) {
                System.err.println("WARNING: Failed subregion page: " + subregion.url());
                continue;
            }

            List<GolfNowDirectoryParser.CourseRow> extracted = GolfNowDirectoryParser.extractCourses(subregion, subregionHtml);
            if (extracted.isEmpty()) {
                extracted = extractCoursesFromFacilityIds(subregion, subregionHtml, config.maxRetries(), config.delayMs(), config.teeTimesSource());
            }
            if (!TEE_TIMES_SOURCE_NONE.equals(config.teeTimesSource())) {
                extracted = resolveTeeTimesUrls(extracted, config.maxRetries(), config.delayMs(), config.teeTimesSource());
            }

            System.out.printf("  -> %d courses%n", extracted.size());
            rows.addAll(extracted);
            sleep(config.delayMs());
        }

        Set<String> seen = new LinkedHashSet<>();
        List<GolfNowDirectoryParser.CourseRow> deduped = new ArrayList<>();
        for (GolfNowDirectoryParser.CourseRow row : rows) {
            String key = (row.region() + "|" + row.subregion() + "|" + row.courseName() + "|" + row.courseUrl()).toLowerCase(Locale.ROOT);
            if (seen.add(key)) {
                deduped.add(row);
            }
        }

        deduped.sort(Comparator
                .comparing(GolfNowDirectoryParser.CourseRow::region)
                .thenComparing(GolfNowDirectoryParser.CourseRow::subregion)
                .thenComparing(GolfNowDirectoryParser.CourseRow::courseName));

        System.out.println("Rows before dedupe: " + rows.size());
        System.out.println("Rows after dedupe: " + deduped.size());

        String csv = GolfNowDirectoryParser.toCsv(deduped);
        if (config.dryRun()) {
            System.out.println("Dry run complete; CSV not written.");
            System.out.println("Preview (first 5 rows):");
            deduped.stream().limit(5).forEach(row ->
                    System.out.printf("  %s | %s | %s | %s | %s%n", row.region(), row.subregion(), row.courseName(), row.courseUrl(), row.teeTimesUrl()));
        } else {
            Path outputPath = Paths.get(config.outputPath());
            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(outputPath, csv);
            System.out.println("CSV written: " + outputPath.toAbsolutePath());
        }

        long teeTimesMissing = deduped.stream().filter(row -> row.teeTimesUrl().isBlank()).count();
        System.out.println("tee_times_url blank rows: " + teeTimesMissing);
    }

    private static Config parseArgs(String[] args) {
        boolean dryRun = false;
        String output = DEFAULT_OUTPUT;
        int limitSubregions = 0;
        int delayMs = 400;
        int maxRetries = 3;
        String teeTimesSource = TEE_TIMES_SOURCE_NONE;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--dry-run" -> dryRun = true;
                case "--output" -> {
                    if (i + 1 >= args.length) {
                        printUsageAndExit("Missing value for --output");
                    }
                    output = args[++i];
                }
                case "--limit-subregions" -> {
                    if (i + 1 >= args.length) {
                        printUsageAndExit("Missing value for --limit-subregions");
                    }
                    limitSubregions = parsePositiveInt(args[++i], "--limit-subregions");
                }
                case "--delay-ms" -> {
                    if (i + 1 >= args.length) {
                        printUsageAndExit("Missing value for --delay-ms");
                    }
                    delayMs = parsePositiveInt(args[++i], "--delay-ms");
                }
                case "--max-retries" -> {
                    if (i + 1 >= args.length) {
                        printUsageAndExit("Missing value for --max-retries");
                    }
                    maxRetries = parsePositiveInt(args[++i], "--max-retries");
                }
                case "--resolve-tee-times" -> teeTimesSource = TEE_TIMES_SOURCE_FACILITY;
                case "--no-resolve-tee-times" -> teeTimesSource = TEE_TIMES_SOURCE_NONE;
                case "--tee-times-source" -> {
                    if (i + 1 >= args.length) {
                        printUsageAndExit("Missing value for --tee-times-source");
                    }
                    teeTimesSource = parseTeeTimesSource(args[++i]);
                }
                case "--help", "-h" -> printUsageAndExit(null);
                default -> printUsageAndExit("Unknown argument: " + arg);
            }
        }

        return new Config(dryRun, output, limitSubregions, delayMs, maxRetries, teeTimesSource);
    }

    private static String parseTeeTimesSource(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (TEE_TIMES_SOURCE_NONE.equals(normalized)
                || TEE_TIMES_SOURCE_NEARBY.equals(normalized)
                || TEE_TIMES_SOURCE_FACILITY.equals(normalized)) {
            return normalized;
        }

        printUsageAndExit("Invalid value for --tee-times-source: " + value + " (expected none, nearby, or facility)");
        return TEE_TIMES_SOURCE_NONE;
    }

    private static int parsePositiveInt(String value, String argName) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 1) {
                printUsageAndExit(argName + " must be >= 1");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            printUsageAndExit("Invalid integer for " + argName + ": " + value);
            return -1;
        }
    }

    private static void printUsageAndExit(String error) {
        if (error != null && !error.isBlank()) {
            System.err.println("ERROR: " + error);
        }
        System.out.println("Usage: jbang scripts/FetchGolfNowDirectoryCsv.java [options]");
        System.out.println("  --dry-run                 Crawl and parse without writing CSV");
        System.out.println("  --output <path>           Output CSV path (default: " + DEFAULT_OUTPUT + ")");
        System.out.println("  --limit-subregions <n>    Limit number of subregions crawled");
        System.out.println("  --delay-ms <n>            Delay between requests in milliseconds");
        System.out.println("  --max-retries <n>         HTTP fetch retries per URL");
        System.out.println("  --tee-times-source <m>    Tee-times mode: none, nearby, or facility (default: none)");
        System.out.println("  --resolve-tee-times       Legacy alias for --tee-times-source facility");
        System.out.println("  --no-resolve-tee-times    Legacy alias for --tee-times-source none");
        System.out.println("  --help                    Show help");
        System.exit(error == null ? 0 : 1);
    }

    private static List<GolfNowDirectoryParser.CourseRow> resolveTeeTimesUrls(List<GolfNowDirectoryParser.CourseRow> rows, int maxRetries, int delayMs, String teeTimesSource) {
        Map<String, String> teeTimesByCourseUrl = new HashMap<>();
        List<GolfNowDirectoryParser.CourseRow> resolved = new ArrayList<>(rows.size());

        for (GolfNowDirectoryParser.CourseRow row : rows) {
            String cached = teeTimesByCourseUrl.get(row.courseUrl());
            if (cached == null) {
                String courseHtml = fetchWithRetry(row.courseUrl(), maxRetries);
                cached = switch (teeTimesSource) {
                    case TEE_TIMES_SOURCE_NEARBY -> GolfNowDirectoryParser.extractNearbyTeeTimesUrlFromCourseHtml(row.courseUrl(), courseHtml);
                    case TEE_TIMES_SOURCE_FACILITY -> GolfNowDirectoryParser.extractTeeTimesUrlFromCourseHtml(row.courseUrl(), courseHtml);
                    default -> "";
                };
                teeTimesByCourseUrl.put(row.courseUrl(), cached == null ? "" : cached);
                sleep(delayMs);
            }

            String resolvedTeeTimes = teeTimesByCourseUrl.getOrDefault(row.courseUrl(), "");
            if (resolvedTeeTimes.isBlank()) {
                // Keep previously discovered value from fallback extraction when resolver yields nothing.
                resolvedTeeTimes = row.teeTimesUrl();
            }

            resolved.add(new GolfNowDirectoryParser.CourseRow(
                    row.region(),
                    row.subregion(),
                    row.courseName(),
                    row.courseUrl(),
                    resolvedTeeTimes
            ));
        }

        return resolved;
    }

    private static List<GolfNowDirectoryParser.CourseRow> extractCoursesFromFacilityIds(
            GolfNowDirectoryParser.Subregion subregion,
            String subregionHtml,
            int maxRetries,
            int delayMs,
            String teeTimesSource
    ) {
        List<String> facilityIds = GolfNowDirectoryParser.extractFacilityIdsFromSubregionHtml(subregionHtml);
        if (facilityIds.isEmpty()) {
            return List.of();
        }

        String facilityTemplate = GolfNowDirectoryParser.extractFacilityLinkTemplate(subregionHtml);
        Map<String, GolfNowDirectoryParser.CourseRow> rowsByCourseKey = new LinkedHashMap<>();

        for (String facilityId : facilityIds) {
            String facilityUrl = absoluteFromTemplate(facilityTemplate, facilityId);
            String facilityHtml = fetchWithRetry(facilityUrl, maxRetries);
            if (facilityHtml == null) {
                System.err.println("WARNING: Failed facility page: " + facilityUrl);
                continue;
            }

            GolfNowDirectoryParser.FacilityCourseInfo info = GolfNowDirectoryParser.extractFacilityCourseInfoFromHtml(facilityUrl, facilityHtml);
            if (info.courseUrl().isBlank() || info.courseName().isBlank()) {
                continue;
            }

            String teeTimes = TEE_TIMES_SOURCE_NONE.equals(teeTimesSource) ? "" : info.teeTimesUrl();
            String key = info.courseUrl().toLowerCase(Locale.ROOT);
            rowsByCourseKey.putIfAbsent(
                    key,
                    new GolfNowDirectoryParser.CourseRow(
                            subregion.region(),
                            subregion.subregion(),
                            info.courseName(),
                            info.courseUrl(),
                            teeTimes
                    )
            );

            sleep(delayMs);
        }

        return new ArrayList<>(rowsByCourseKey.values());
    }

    private static String absoluteFromTemplate(String template, String facilityId) {
        String resolved = template.replace("~facilityid~", facilityId);
        if (resolved.startsWith("http://") || resolved.startsWith("https://")) {
            return resolved;
        }
        if (resolved.startsWith("/")) {
            return "https://www.golfnow.co.uk" + resolved;
        }
        return "https://www.golfnow.co.uk/" + resolved;
    }

    private static String fetchWithRetry(String url, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                var response = Jsoup.connect(url)
                        .userAgent(USER_AGENT)
                        .timeout(HTTP_TIMEOUT_MS)
                        .referrer("https://www.golfnow.co.uk/")
                        .header("Accept-Language", "en-GB,en;q=0.9")
                        .header("Connection", "keep-alive")
                        .ignoreHttpErrors(true)
                        .execute();

                if (response.statusCode() >= 400) {
                    System.err.printf("HTTP %d fetching %s (attempt %d/%d)%n", response.statusCode(), url, attempt, maxRetries);
                } else {
                    return response.body();
                }
            } catch (SocketTimeoutException timeoutException) {
                System.err.printf("Timeout fetching %s (attempt %d/%d)%n", url, attempt, maxRetries);
            } catch (IOException ioException) {
                System.err.printf("HTTP error fetching %s (attempt %d/%d): %s%n", url, attempt, maxRetries, ioException.getMessage());
            }

            if (attempt < maxRetries) {
                sleep(Math.min(1000L * attempt, 3000));
            }
        }

        return null;
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

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
