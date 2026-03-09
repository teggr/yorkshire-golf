///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.16.1
//DEPS com.fasterxml.jackson.core:jackson-databind:2.16.1
//DEPS org.slf4j:slf4j-simple:2.0.9
//JAVA 21

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * FetchAddresses - Automatically fetches addresses/postcodes from golf course websites
 * and populates the 'address' field in each course YAML file.
 *
 * Run from repo root with: jbang scripts/FetchAddresses.java
 * Validation run (no writes): jbang scripts/FetchAddresses.java --dry-run --limit 5
 * Cleanup existing noisy addresses: jbang scripts/FetchAddresses.java --cleanup-existing
 * Apply to all eligible courses: jbang scripts/FetchAddresses.java
 *
 * Only processes courses that:
 * - Are not marked as closed
 * - Have a website URL
 * - Do not already have an address
 *
 * The address field is optional; if it cannot be found the YAML is left unchanged.
 */
public class FetchAddresses {

    private static final String COURSES_PATH = "src/main/resources/courses";

    // UK postcode pattern (full and partial)
    private static final Pattern POSTCODE_PATTERN = Pattern.compile(
        "\\b([A-Z]{1,2}[0-9]{1,2}[A-Z]?\\s*[0-9][A-Z]{2})\\b",
        Pattern.CASE_INSENSITIVE
    );

    // Maximum characters of address context to extract before a postcode
    private static final int MAX_ADDRESS_CONTEXT_LENGTH = 100;
    private static final int TRAILING_ADDRESS_WINDOW = 90;

    // Common contact/location page path fragments to try in addition to the homepage
    private static final List<String> CONTACT_PATHS = List.of(
        "/contact", "/contact-us", "/find-us", "/location", "/about", "/about-us",
        "/visit-us", "/getting-here", "/directions"
    );

    record Config(boolean dryRun, int limit, boolean cleanupExisting) {}

    public static void main(String[] args) throws IOException {
        Config config = parseArgs(args);

        Path coursesDir = Paths.get(COURSES_PATH);
        if (!Files.exists(coursesDir)) {
            System.err.println("ERROR: Courses directory not found: " + coursesDir.toAbsolutePath());
            System.exit(1);
        }

        List<Path> courseFiles = Files.list(coursesDir)
            .filter(p -> p.toString().endsWith(".yaml"))
            .sorted()
            .collect(Collectors.toList());

        if (config.cleanupExisting()) {
            runAddressCleanup(courseFiles, config);
            return;
        }

        System.out.println("Found " + courseFiles.size() + " course files");
        if (config.dryRun()) {
            System.out.println("Mode: DRY RUN (no files will be changed)");
        }
        if (config.limit() > 0) {
            System.out.println("Limit: " + config.limit() + " eligible courses");
        }

        int fileIndex = 0;
        int updated = 0;
        int skipped = 0;
        int failed = 0;
        int processedEligible = 0;

        for (Path file : courseFiles) {
            fileIndex++;
            String yaml = Files.readString(file);
            CourseInfo info = parseCourse(yaml);

            // Skip closed courses
            if (info.closed) {
                skipped++;
                continue;
            }

            // Skip courses without a website
            if (info.website == null || info.website.isEmpty()) {
                skipped++;
                continue;
            }

            // Skip courses that already have an address
            if (info.address != null && !info.address.isEmpty()) {
                skipped++;
                continue;
            }

            if (config.limit() > 0 && processedEligible >= config.limit()) {
                break;
            }

            processedEligible++;

            System.out.printf("[%d/%d] %s%n", fileIndex, courseFiles.size(), info.name);
            System.out.println("  Website: " + info.website);

            String foundAddress = fetchAddress(info.website);

            if (foundAddress != null) {
                System.out.println("  Found address: " + foundAddress);
                if (!config.dryRun()) {
                    String updatedYaml = updateAddressInYaml(yaml, foundAddress);
                    Files.writeString(file, updatedYaml);
                }
                updated++;
            } else {
                System.out.println("  No address found");
                failed++;
            }

            // Small delay to be respectful of servers
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("\n=== Summary ===");
        System.out.println("Eligible processed: " + processedEligible);
        System.out.println("Updated: " + updated);
        System.out.println("Skipped (closed/no website/already has address): " + skipped);
        System.out.println("No address found: " + failed);
    }

    private static Config parseArgs(String[] args) {
        boolean dryRun = false;
        int limit = 0;
        boolean cleanupExisting = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if ("--dry-run".equals(arg)) {
                dryRun = true;
                continue;
            }

            if ("--cleanup-existing".equals(arg)) {
                cleanupExisting = true;
                continue;
            }

            if ("--limit".equals(arg)) {
                if (i + 1 >= args.length) {
                    printUsageAndExit("Missing value for --limit");
                }
                String limitValue = args[++i];
                try {
                    limit = Integer.parseInt(limitValue);
                } catch (NumberFormatException ex) {
                    printUsageAndExit("Invalid integer for --limit: " + limitValue);
                }
                if (limit < 1) {
                    printUsageAndExit("--limit must be >= 1");
                }
                continue;
            }

            if ("--help".equals(arg) || "-h".equals(arg)) {
                printUsageAndExit(null);
            }

            printUsageAndExit("Unknown argument: " + arg);
        }

        return new Config(dryRun, limit, cleanupExisting);
    }

    private static void printUsageAndExit(String error) {
        if (error != null && !error.isBlank()) {
            System.err.println("ERROR: " + error);
        }
        System.out.println("Usage: jbang scripts/FetchAddresses.java [--dry-run] [--limit <n>] [--cleanup-existing]");
        System.out.println("  --dry-run   Validate extraction without writing YAML changes");
        System.out.println("  --limit n   Process up to n eligible courses");
        System.out.println("  --cleanup-existing   Clean noisy text in existing address fields");
        System.out.println("  --help      Show this help message");
        System.exit(error == null ? 0 : 1);
    }

    private static void runAddressCleanup(List<Path> courseFiles, Config config) throws IOException {
        System.out.println("Found " + courseFiles.size() + " course files");
        System.out.println("Mode: CLEANUP EXISTING ADDRESSES" + (config.dryRun() ? " (dry-run)" : ""));
        if (config.limit() > 0) {
            System.out.println("Limit: " + config.limit() + " address fields");
        }

        int processed = 0;
        int updated = 0;
        int unchanged = 0;

        for (Path file : courseFiles) {
            String yaml = Files.readString(file);
            CourseInfo info = parseCourse(yaml);

            if (info.address == null || info.address.isBlank()) {
                continue;
            }

            if (config.limit() > 0 && processed >= config.limit()) {
                break;
            }
            processed++;

            String cleaned = cleanupExistingAddress(info.address);
            if (cleaned.equals(info.address)) {
                unchanged++;
                continue;
            }

            System.out.println("Cleaned: " + info.name);
            System.out.println("  Old: " + info.address);
            System.out.println("  New: " + cleaned);

            if (!config.dryRun()) {
                String updatedYaml = updateAddressInYaml(yaml, cleaned);
                Files.writeString(file, updatedYaml);
            }
            updated++;
        }

        System.out.println("\n=== Cleanup Summary ===");
        System.out.println("Address fields processed: " + processed);
        System.out.println("Updated: " + updated);
        System.out.println("Unchanged: " + unchanged);
    }

    private static String cleanupExistingAddress(String address) {
        if (address == null || address.isBlank()) {
            return address;
        }

        if (!containsNoiseMarker(address)) {
            return normalizeAddressText(address);
        }

        String cleaned = address
            .replaceAll("(?i)\\bnbsp\\b", " ")
            .replaceAll("(?i)\\bprotected\\b", " ")
            .replaceAll("(?i)\\bselect\\s+page\\b", " ")
            .replaceAll("(?i)\\bcurrent\\s+slide\\b", " ")
            .replaceAll("(?i)\\bslide\\s+\\d+\\b", " ")
            .replaceAll("(?i)\\blogin\\s+home\\b", " ")
            .replaceAll("(?i)\\bclick\\s+here\\s+for\\s+visitor\\s+booking\\b", " ")
            .replaceAll("(?i)\\bclick\\s+here\\s+for\\s+more\\s+information\\b", " ")
            .replaceAll("(?i)\\bclick\\s+for\\s+more\\b", " ")
            .replaceAll("(?i)\\bnavigation\\s+find\\s+a\\s+centre\\s+home\\b", " ")
            .replaceAll("(?i)\\bopen\\s+today\\s+\\d+am\\s*-\\s*\\d+pm\\b", " ")
            .replaceAll("(?i)\\bfind\\s+out\\s+more\\b", " ")
            .replaceAll("(?i)\\bjoin\\s+become\\s+a\\s+member\\b", " ")
            .replaceAll("(?i)\\bfollow\\s+us\\s+on\\s+facebook\\b", " ")
            .replaceAll("(?i)\\bvisitors\\s+pro\\s+coaching\\s+academy\\s+policies\\b", " ")
            .replaceAll("(?i)\\bread\\s+more\\s+footer\\s+visit\\s+us\\b", " ")
            .replaceAll("(?i)\\bact\\s+us\\b", " ")
            .replaceAll("(?i)\\baddress\\b", " ")
            .replaceAll("(?i)\\bpostcode\\b", " ");

        Matcher matcher = POSTCODE_PATTERN.matcher(cleaned);
        if (matcher.find()) {
            int start = Math.max(0, matcher.start() - MAX_ADDRESS_CONTEXT_LENGTH);
            cleaned = cleaned.substring(start, matcher.end());
        }

        cleaned = cleaned
            .replaceAll("(?i)^(?:\\d+\\s+)+", "")
            .replaceAll("(?i)^(?:the\\s+)?(?:world\\s+handicap\\s+system\\s+for\\s+details\\s+)", "")
            .replaceAll("(?i)^(?:fri\\s+\\d+\\s+)", "")
            .replaceAll("(?i)^(?:t\\s+head\\s+chef\\s+)", "")
            .replaceAll("(?i)^(?:con)?tact\\s+us\\s+", "")
            .replaceAll("(?i)^now\\s+", "")
            .replaceAll("(?i)^close\\s+", "")
            .replaceAll("(?i)^confirm\\s+", "")
            .replaceAll("(?i)^info\\s+", "")
            .replaceAll("(?i)^directions\\s+", "")
            .replaceAll("(?i)^entry\\s+short\\s+game\\s+area\\s+", "")
            .replaceAll("(?i)^to\\s+the\\s+course\\s+info\\s+", "")
            .replaceAll("(?i)^details\\s+and\\s+we\\s+will\\s+aim\\s+to\\s+respond\\s+as\\s+soon\\s+as\\s+possible\\s+", "")
            .replaceAll("(?i)^book\\s+simulator\\s+where\\s+we\\s+are\\s+", "")
            .replaceAll("(?i)^refreshments\\s+available\\s+in\\s+the\\s+clubhouse\\s+book\\s+now\\s+where\\s+we\\s+are\\s+", "")
            .replaceAll("(?i)^(?:tyn\\s+gee\\s+seniors\\s+geoff\\s+carswell\\s+)", "");

        return normalizeAddressText(cleaned);
    }

    private static boolean containsNoiseMarker(String address) {
        return address.matches("(?is).*(login|click|select\\s+page|slide|nbsp|protected|navigation\\s+find\\s+a\\s+centre|open\\s+today|postcode|act\\s+us|tact\\s+us|book\\s+simulator\\s+where\\s+we\\s+are|find\\s+out\\s+more|join\\s+become\\s+a\\s+member|follow\\s+us\\s+on\\s+facebook|visitors\\s+pro\\s+coaching\\s+academy\\s+policies|read\\s+more\\s+footer\\s+visit\\s+us|^\\s*(now|close|confirm|info|directions|entry\\s+short\\s+game\\s+area|to\\s+the\\s+course\\s+info)\\b|details\\s+and\\s+we\\s+will\\s+aim\\s+to\\s+respond\\s+as\\s+soon\\s+as\\s+possible).*" );
    }

    private static String normalizeAddressText(String value) {
        return value
            .replaceAll("[^A-Za-z0-9,\\-\\s]", " ")
            .replaceAll("\\s+,", ",")
            .replaceAll(",\\s+", ", ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    /**
     * Tries to find an address or postcode on the course website.
     * Checks the homepage first, then common contact/location pages.
     */
    private static String fetchAddress(String websiteUrl) {
        // Normalise base URL
        String base = websiteUrl.endsWith("/") ? websiteUrl.substring(0, websiteUrl.length() - 1) : websiteUrl;

        // Try homepage first
        String html = fetchPage(base);
        if (html != null) {
            String address = extractAddress(html);
            if (address != null) return address;
        }

        // Try common contact/location paths
        for (String path : CONTACT_PATHS) {
            html = fetchPage(base + path);
            if (html != null) {
                String address = extractAddress(html);
                if (address != null) return address;
            }
        }

        return null;
    }

    /**
     * Attempts to extract an address from HTML content.
     * Looks for UK postcodes and surrounding address-like context.
     */
    private static String extractAddress(String html) {
        if (html == null) return null;

        // Strip HTML tags for text analysis
        String text = html
            .replaceAll("<script[^>]*>[\\s\\S]*?</script>", " ")
            .replaceAll("<style[^>]*>[\\s\\S]*?</style>", " ")
            .replaceAll("<[^>]+>", " ")
            .replaceAll("\\s+", " ")
            .trim();

        // Search for UK postcode
        Matcher matcher = POSTCODE_PATTERN.matcher(text);
        if (matcher.find()) {
            String postcode = matcher.group(1).toUpperCase().replaceAll("\\s+", " ").trim();

            // Try to extract surrounding address context (up to MAX_ADDRESS_CONTEXT_LENGTH chars before postcode)
            int postcodeStart = matcher.start();
            int contextStart = Math.max(0, postcodeStart - MAX_ADDRESS_CONTEXT_LENGTH);
            String before = text.substring(contextStart, postcodeStart).trim();

            // Clean up the context - take the last address-like fragment
            String addressContext = extractAddressContext(before, postcode);
            return addressContext;
        }

        return null;
    }

    /**
     * Extracts a clean address string from the text immediately before a postcode.
     */
    private static String extractAddressContext(String before, String postcode) {
        String cleaned = before
            .replaceAll("&[A-Za-z#0-9]+;", " ")
            .replaceAll("(?i)\\b[\\w.%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b", " ")
            .replaceAll("(?i)\\b(click\\s*to\\s*call|click\\s*to\\s*email|email\\s*us|get\\s*in\\s*touch|find\\s*us|contact\\s*us|contact|email|tel|phone)\\b[:\\s]*", " ")
            .replaceAll("\\b\\+?\\d[\\d\\s()\\-]{6,}\\b", " ")
            .replaceAll("©", " ")
            .replaceAll("(?i)\\bcopyright\\b", " ")
            .replaceAll("\\s+", " ")
            .trim();

        if (cleaned.length() > TRAILING_ADDRESS_WINDOW) {
            cleaned = cleaned.substring(cleaned.length() - TRAILING_ADDRESS_WINDOW).trim();
            int firstSpace = cleaned.indexOf(' ');
            if (firstSpace > 0 && firstSpace < 15) {
                cleaned = cleaned.substring(firstSpace + 1).trim();
            }
        }

        if (cleaned.matches("(?i)^.*\\b(ltd|limited)\\b,\\s*.*$")) {
            int commaIndex = cleaned.indexOf(',');
            if (commaIndex > 0 && commaIndex < 35 && commaIndex < cleaned.length() - 1) {
                cleaned = cleaned.substring(commaIndex + 1).trim();
            }
        }

        cleaned = cleaned
            .replaceAll("^[^A-Za-z0-9]+", "")
            .replaceAll("[^A-Za-z0-9,\\-\\s]", " ")
            .replaceAll("\\s+,", ",")
            .replaceAll(",\\s+", ", ")
            .replaceAll("\\s+", " ")
            .trim();

        if (cleaned.isEmpty()) {
            return postcode;
        }

        if (cleaned.length() > MAX_ADDRESS_CONTEXT_LENGTH) {
            cleaned = cleaned.substring(cleaned.length() - MAX_ADDRESS_CONTEXT_LENGTH).trim();
            int comma = cleaned.indexOf(',');
            if (comma > 0 && comma < 30) {
                cleaned = cleaned.substring(comma + 1).trim();
            }
        }

        return cleaned + " " + postcode;
    }

    /**
     * Fetches the HTML content of a URL with a timeout.
     */
    private static String fetchPage(String urlStr) {
        try {
            URL url = URI.create(urlStr).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (compatible; YorkshireGolfBot/1.0; address lookup)");
            conn.setInstanceFollowRedirects(true);

            int status = conn.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                return new String(conn.getInputStream().readAllBytes());
            }
        } catch (Exception e) {
            // Silently ignore connection errors
        }
        return null;
    }

    private static CourseInfo parseCourse(String yaml) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = mapper.readValue(yaml, Map.class);

        String name = (String) data.get("name");
        String website = (String) data.get("website");
        String address = (String) data.get("address");

        boolean closed = false;
        Object closedObj = data.get("closed");
        if (closedObj instanceof Boolean b) {
            closed = b;
        } else if (closedObj instanceof String s) {
            closed = "true".equalsIgnoreCase(s);
        }

        return new CourseInfo(name, website, address, closed);
    }

    private static String updateAddressInYaml(String yamlContent, String newAddress) {
        boolean hasAddressField = yamlContent.contains("address:");
        String[] lines = yamlContent.split("\n");
        StringBuilder result = new StringBuilder();
        boolean foundAddress = false;
        boolean addedAddress = false;

        for (String line : lines) {
            if (line.trim().startsWith("address:")) {
                if (!foundAddress) {
                    String indent = line.substring(0, line.indexOf("address:"));
                    result.append(indent).append("address: \"").append(newAddress).append("\"");
                    foundAddress = true;
                    result.append("\n");
                }
                continue;
            }

            result.append(line);

            if (!hasAddressField && !addedAddress && line.trim().startsWith("name:")) {
                String indent = "";
                if (line.indexOf("name:") > 0) {
                    indent = line.substring(0, line.indexOf("name:"));
                }
                result.append("\n").append(indent).append("address: \"").append(newAddress).append("\"");
                addedAddress = true;
            }

            result.append("\n");
        }

        return result.toString();
    }

    record CourseInfo(String name, String website, String address, boolean closed) {}
}
