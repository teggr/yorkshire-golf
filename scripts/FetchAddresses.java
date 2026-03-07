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

    // Common contact/location page path fragments to try in addition to the homepage
    private static final List<String> CONTACT_PATHS = List.of(
        "/contact", "/contact-us", "/find-us", "/location", "/about", "/about-us",
        "/visit-us", "/getting-here", "/directions"
    );

    public static void main(String[] args) throws IOException {
        Path coursesDir = Paths.get(COURSES_PATH);
        if (!Files.exists(coursesDir)) {
            System.err.println("ERROR: Courses directory not found: " + coursesDir.toAbsolutePath());
            System.exit(1);
        }

        List<Path> courseFiles = Files.list(coursesDir)
            .filter(p -> p.toString().endsWith(".yaml"))
            .sorted()
            .collect(Collectors.toList());

        System.out.println("Found " + courseFiles.size() + " course files");

        int fileIndex = 0;
        int updated = 0;
        int skipped = 0;
        int failed = 0;

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

            System.out.printf("[%d/%d] %s%n", fileIndex, courseFiles.size(), info.name);
            System.out.println("  Website: " + info.website);

            String foundAddress = fetchAddress(info.website);

            if (foundAddress != null) {
                System.out.println("  Found address: " + foundAddress);
                String updatedYaml = updateAddressInYaml(yaml, foundAddress);
                Files.writeString(file, updatedYaml);
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
        System.out.println("Updated: " + updated);
        System.out.println("Skipped (closed/no website/already has address): " + skipped);
        System.out.println("No address found: " + failed);
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
        // Split by common separators and take the most relevant trailing portion
        String[] parts = before.split("[|•·\\n\\r]");
        String lastPart = parts[parts.length - 1].trim();

        // Remove leading/trailing non-address chars
        lastPart = lastPart.replaceAll("^[^A-Za-z0-9]+", "").trim();

        if (lastPart.isEmpty()) {
            return postcode;
        }

        // Limit to a reasonable address length
        if (lastPart.length() > MAX_ADDRESS_CONTEXT_LENGTH) {
            // Take only the last MAX_ADDRESS_CONTEXT_LENGTH characters
            lastPart = lastPart.substring(lastPart.length() - MAX_ADDRESS_CONTEXT_LENGTH).trim();
            // Try to start at a sensible word boundary
            int comma = lastPart.indexOf(',');
            if (comma > 0 && comma < 30) {
                lastPart = lastPart.substring(comma + 1).trim();
            }
        }

        return lastPart + " " + postcode;
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
