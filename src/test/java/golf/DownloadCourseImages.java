package golf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Utility to scrape course websites for large images suitable for display.
 * Downloads images to src/main/resources/static/images/courses/ and updates
 * the corresponding YAML files with the mainImageUrl field.
 *
 * Run as a main class to scrape images for all courses with a website.
 */
@Slf4j
public class DownloadCourseImages {

    private static final String COURSES_YAML_DIR = "src/main/resources/courses";
    private static final String STATIC_IMAGES_DIR = "src/main/resources/static/images/courses";
    private static final String IMAGE_URL_PREFIX = "/images/courses/";
    private static final int TIMEOUT_SECONDS = 15;

    public static void main(String[] args) throws Exception {

        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));

        Path coursesDir = Path.of(COURSES_YAML_DIR);
        Path imagesDir = Path.of(STATIC_IMAGES_DIR);
        Files.createDirectories(imagesDir);

        HttpClient httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();

        // Limit to first N courses with a website for initial validation (default: 3)
        int limit = 3;
        if (args.length > 0) {
            try {
                limit = Integer.parseInt(args[0]);
                if (limit <= 0) throw new IllegalArgumentException("limit must be positive");
            } catch (NumberFormatException e) {
                log.error("Invalid limit argument '{}': must be a positive integer", args[0]);
                System.exit(1);
            }
        }
        int processed = 0;

        try (var stream = Files.list(coursesDir)) {
            for (Path yamlFile : (Iterable<Path>) stream.sorted()::iterator) {
                if (!yamlFile.toString().endsWith(".yaml")) continue;

                @SuppressWarnings("unchecked")
                Map<String, Object> courseData = yamlMapper.readValue(yamlFile.toFile(), Map.class);

                String website = (String) courseData.get("website");
                if (website == null || website.isBlank()) continue;
                if (courseData.get("mainImageUrl") != null) {
                    log.info("Skipping {} - already has mainImageUrl", yamlFile.getFileName());
                    continue;
                }

                if (processed >= limit) break;

                log.info("Processing: {} -> {}", yamlFile.getFileName(), website);

                try {
                    Optional<String> imageUrl = findBestImageUrl(website);
                    if (imageUrl.isEmpty()) {
                        log.warn("No suitable image found for {}", website);
                        continue;
                    }

                    String imgUrl = imageUrl.get();
                    log.info("Found image: {}", imgUrl);

                    String fileName = yamlFile.getFileName().toString().replace(".yaml", "") + imageExtension(imgUrl);
                    Path destPath = imagesDir.resolve(fileName);

                    downloadImage(httpClient, imgUrl, destPath);

                    String mainImageUrl = IMAGE_URL_PREFIX + fileName;
                    Map<String, Object> updatedData = new LinkedHashMap<>(courseData);
                    updatedData.put("mainImageUrl", mainImageUrl);

                    yamlMapper.writeValue(yamlFile.toFile(), updatedData);
                    log.info("Updated {} with mainImageUrl: {}", yamlFile.getFileName(), mainImageUrl);
                    processed++;

                } catch (Exception e) {
                    log.error("Failed to process {}: {}", yamlFile.getFileName(), e.getMessage());
                }
            }
        }

        log.info("Done. Processed {} courses.", processed);
    }

    /**
     * Finds the best image URL for a given website URL.
     * Prefers Open Graph images, then Twitter card images, then the first large img tag.
     */
    static Optional<String> findBestImageUrl(String websiteUrl) {
        try {
            Document doc = Jsoup.connect(websiteUrl)
                    .userAgent("Mozilla/5.0 (compatible; YorkshireGolfBot/1.0)")
                    .timeout(TIMEOUT_SECONDS * 1000)
                    .get();

            // 1. Try Open Graph image (best for hero/display images)
            Element ogImage = doc.selectFirst("meta[property=og:image]");
            if (ogImage != null && !ogImage.attr("content").isBlank()) {
                return Optional.of(resolveUrl(websiteUrl, ogImage.attr("content")));
            }

            // 2. Try Twitter card image
            Element twitterImage = doc.selectFirst("meta[name=twitter:image]");
            if (twitterImage != null && !twitterImage.attr("content").isBlank()) {
                return Optional.of(resolveUrl(websiteUrl, twitterImage.attr("content")));
            }

            // 3. Fall back to largest img tag (by width attribute or src heuristics)
            Elements imgs = doc.select("img[src]");
            for (Element img : imgs) {
                String src = img.attr("abs:src");
                String widthAttr = img.attr("width");
                int width = widthAttr.isBlank() ? 0 : parseIntSafe(widthAttr);
                if (width >= 600 || isLikelyHeroImage(img)) {
                    return Optional.of(src);
                }
            }

        } catch (Exception e) {
            log.warn("Could not fetch {}: {}", websiteUrl, e.getMessage());
        }
        return Optional.empty();
    }

    private static boolean isLikelyHeroImage(Element img) {
        String cls = img.className().toLowerCase();
        String id = img.id().toLowerCase();
        String src = img.attr("src").toLowerCase();
        return cls.contains("hero") || cls.contains("banner") || cls.contains("header")
                || id.contains("hero") || id.contains("banner")
                || src.contains("hero") || src.contains("banner") || src.contains("header");
    }

    private static String resolveUrl(String baseUrl, String url) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        try {
            return URI.create(baseUrl).resolve(url).toString();
        } catch (Exception e) {
            return url;
        }
    }

    private static void downloadImage(HttpClient client, String imageUrl, Path dest) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .header("User-Agent", "Mozilla/5.0 (compatible; YorkshireGolfBot/1.0)")
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .GET()
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " downloading " + imageUrl);
        }
        try (InputStream in = response.body()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        log.info("Downloaded image to {}", dest);
    }

    private static String imageExtension(String url) {
        String lower = url.toLowerCase().split("[?#]")[0];
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return ".jpg";
        if (lower.endsWith(".png")) return ".png";
        if (lower.endsWith(".webp")) return ".webp";
        if (lower.endsWith(".gif")) return ".gif";
        return ".jpg";
    }

    private static int parseIntSafe(String s) {
        try {
            // Strip common CSS suffixes like 'px' before parsing
            return Integer.parseInt(s.trim().replaceAll("[^0-9].*$", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
