///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javalin:javalin:5.6.3
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.16.1
//DEPS com.fasterxml.jackson.core:jackson-databind:2.16.1
//DEPS org.slf4j:slf4j-simple:2.0.9
//DEPS com.twelvemonkeys.imageio:imageio-webp:3.10.1
//JAVA 21

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.IIOImage;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class CourseAudit {

    private static final Logger log = LoggerFactory.getLogger(CourseAudit.class);

    private static final int PORT = 7070;
    private static final String COURSES_PATH = "src/main/resources/courses";
    private static final String IMAGES_PATH = "src/main/resources/static/images/courses";
    private static final String THUMBS_PATH = "src/main/resources/static/images/courses/thumbs";
    private static final String PLACEHOLDER_IMAGE = "/images/courses/placeholder-course.jpg";
    private static final int THUMB_WIDTH = 600;
    private static final float THUMB_JPEG_QUALITY = 0.85f;
    private static final Duration GEOCODE_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration GEOCODE_REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final String SHOW_CLOSED_OPTION = "--show-closed";
    
    private static final FileNavigator fileNavigator = new FileNavigator();
    private static final Validator validator = new Validator();

    public static void main(String[] args) throws IOException {
        boolean showClosed = false;
        for (String arg : args) {
            if (SHOW_CLOSED_OPTION.equals(arg)) {
                showClosed = true;
            } else {
                System.err.println("Unknown option: " + arg);
                System.err.println("Usage: jbang scripts/CourseAudit.java [--show-closed]");
                System.exit(1);
            }
        }

        // Verify directories exist
        Path coursesDir = Paths.get(COURSES_PATH);
        Path imagesDir = Paths.get(IMAGES_PATH);
        
        if (!Files.exists(coursesDir)) {
            System.err.println("ERROR: Courses directory not found: " + coursesDir.toAbsolutePath());
            System.exit(1);
        }
        
        if (!Files.exists(imagesDir)) {
            System.err.println("WARNING: Images directory not found: " + imagesDir.toAbsolutePath());
        }
        
        // Load course files
        fileNavigator.loadCourseFiles(coursesDir, showClosed);
        if (showClosed) {
            System.out.println("Loaded " + fileNavigator.getTotalFiles() + " course YAML files (including closed)");
        } else {
            System.out.println("Loaded " + fileNavigator.getTotalFiles() + " open course YAML files (" + fileNavigator.getClosedFilteredOutCount() + " closed hidden; use --show-closed to include)");
        }
        
        // Create Javalin app
        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
        }).start(PORT);
        
        // Routes
        app.get("/", CourseAudit::handleIndex);
        app.get("/image-proxy", CourseAudit::handleImageProxy);
        app.post("/next", CourseAudit::handleNext);
        app.post("/previous", CourseAudit::handlePrevious);
        app.post("/update-website", CourseAudit::handleUpdateWebsite);
        app.post("/update-golfnow-url", CourseAudit::handleUpdateGolfNowUrl);
        app.post("/update-stay-image", CourseAudit::handleUpdateStayImage);
        app.post("/update-closed", CourseAudit::handleUpdateClosed);
        app.post("/update-play-and-stay", CourseAudit::handleUpdatePlayAndStay);
        app.post("/update-top100", CourseAudit::handleUpdateTop100);
        app.post("/update-next100", CourseAudit::handleUpdateNext100);
        app.post("/update-address", CourseAudit::handleUpdateAddress);
        app.post("/download-image", CourseAudit::handleDownloadImage);
        app.post("/generate-thumbnails", CourseAudit::handleGenerateThumbnails);
        app.post("/jump-to-letter", CourseAudit::handleJumpToLetter);
        app.post("/search-course", CourseAudit::handleSearchCourse);
        app.post("/geocode-current", CourseAudit::handleGeocodeCurrent);
        app.post("/geocode-all", CourseAudit::handleGeocodeAll);
        app.post("/compute-nearby-current", CourseAudit::handleComputeNearbyCurrent);
        app.post("/compute-nearby", CourseAudit::handleComputeNearby);
        app.get("/courses-list", CourseAudit::handleCoursesList);
        
        System.out.println("\n===========================================");
        System.out.println("Course Audit Webapp running at:");
        System.out.println("  http://localhost:" + PORT);
        System.out.println("===========================================\n");
        
        // Open browser
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI("http://localhost:" + PORT));
            }
        } catch (Exception e) {
            System.err.println("Could not open browser: " + e.getMessage());
        }
    }
    
    private static void handleIndex(Context ctx) {
        try {
            Path currentFile = fileNavigator.getCurrentFile();
            if (currentFile == null) {
                ctx.html("<h1>No course files found</h1>");
                return;
            }
            
            String yamlContent = Files.readString(currentFile);
            String fileName = currentFile.getFileName().toString();
            
            // Parse and validate
            CourseData course = parseCourse(yamlContent);
            ValidationResult websiteValidation = validator.validateWebsite(course.website);
            ValidationResult imageValidation = validator.validateImage(course.mainImageUrl);
            ValidationResult stayImageValidation = validator.validateImage(course.stayImageUrl);
            
            ctx.html(renderHTML(fileName, yamlContent, course, websiteValidation, imageValidation, stayImageValidation, null));
        } catch (Exception e) {
            e.printStackTrace(); // Log full stack trace
            ctx.html(renderError("Error loading course", e.getClass().getName() + ": " + e.getMessage() + "\\n" + 
                (e.getCause() != null ? "Caused by: " + e.getCause().getMessage() : "")));
        }
    }
    
    private static void handleImageProxy(Context ctx) {
        String imagePath = ctx.queryParam("path");
        if (imagePath == null || !imagePath.startsWith("/images/courses/")) {
            ctx.status(404).result("Invalid image path");
            return;
        }
        
        // Convert web path to filesystem path
        String filename = imagePath.substring("/images/courses/".length());
        Path imageFile = Paths.get(IMAGES_PATH, filename);
        
        if (!Files.exists(imageFile)) {
            ctx.status(404).result("Image not found");
            return;
        }
        
        try {
            byte[] bytes = Files.readAllBytes(imageFile);
            String contentType = Files.probeContentType(imageFile);
            if (contentType == null) {
                contentType = "image/jpeg"; // default
            }
            ctx.contentType(contentType).result(bytes);
        } catch (IOException e) {
            ctx.status(500).result("Error reading image: " + e.getMessage());
        }
    }
    
    private static void handleNext(Context ctx) {
        fileNavigator.moveNext();
        ctx.redirect("/");
    }
    
    private static void handlePrevious(Context ctx) {
        fileNavigator.movePrevious();
        ctx.redirect("/");
    }
    
    private static void handleUpdateWebsite(Context ctx) {
        try {
            String newWebsiteUrl = ctx.formParam("newWebsiteUrl");
            if (newWebsiteUrl == null) {
                newWebsiteUrl = ""; // Allow clearing the website
            }
            newWebsiteUrl = newWebsiteUrl.trim();
            
            Path currentFile = fileNavigator.getCurrentFile();
            String yamlContent = Files.readString(currentFile);
            
            // Update YAML with new website URL
            String updatedYaml = updateWebsiteUrlInYaml(yamlContent, newWebsiteUrl);
            
            // Save the updated YAML
            Files.writeString(currentFile, updatedYaml);
            
            // Redirect to reload the page
            ctx.redirect("/");
        } catch (Exception e) {
            ctx.html(renderError("Update error", e.getMessage()));
        }
    }
    
    private static void handleUpdateGolfNowUrl(Context ctx) {
        try {
            String newGolfNowUrl = ctx.formParam("newGolfNowUrl");
            if (newGolfNowUrl == null) {
                newGolfNowUrl = ""; // Allow clearing the URL
            }
            newGolfNowUrl = newGolfNowUrl.trim();
            
            Path currentFile = fileNavigator.getCurrentFile();
            String yamlContent = Files.readString(currentFile);
            
            // Update YAML with new GolfNow URL
            String updatedYaml = updateGolfNowUrlInYaml(yamlContent, newGolfNowUrl);
            
            // Save the updated YAML
            Files.writeString(currentFile, updatedYaml);
            
            // Redirect to reload the page
            ctx.redirect("/");
        } catch (Exception e) {
            ctx.html(renderError("Update error", e.getMessage()));
        }
    }
    
    private static void handleUpdateClosed(Context ctx) {
        try {
            // Get all values for "closed" parameter
            List<String> closedParams = ctx.formParams("closed");
            // If the checkbox is checked, it will send "true". If unchecked, only hidden field sends "false"
            // We check if any of the values is "true"
            boolean isClosed = closedParams != null && closedParams.contains("true");
            
            Path currentFile = fileNavigator.getCurrentFile();
            String yamlContent = Files.readString(currentFile);
            
            // Update YAML with closed status
            String updatedYaml = updateClosedInYaml(yamlContent, isClosed);
            
            // Save the updated YAML
            Files.writeString(currentFile, updatedYaml);
            
            // Redirect to reload the page
            ctx.redirect("/");
        } catch (Exception e) {
            ctx.html(renderError("Update error", e.getMessage()));
        }
    }

    private static void handleUpdatePlayAndStay(Context ctx) {
        try {
            List<String> playAndStayParams = ctx.formParams("playAndStay");
            boolean isPlayAndStay = playAndStayParams != null && playAndStayParams.contains("true");

            Path currentFile = fileNavigator.getCurrentFile();
            String yamlContent = Files.readString(currentFile);

            String updatedYaml = updatePlayAndStayInYaml(yamlContent, isPlayAndStay);

            Files.writeString(currentFile, updatedYaml);

            ctx.redirect("/");
        } catch (Exception e) {
            ctx.html(renderError("Update error", e.getMessage()));
        }
    }

    private static void handleUpdateTop100(Context ctx) {
        handleUpdateOptionalRankField(ctx, "top100", "newTop100");
    }

    private static void handleUpdateNext100(Context ctx) {
        handleUpdateOptionalBooleanField(ctx, "next100", "next100");
    }

    private static void handleUpdateOptionalBooleanField(Context ctx, String fieldName, String formFieldName) {
        try {
            List<String> fieldValues = ctx.formParams(formFieldName);
            boolean enabled = fieldValues != null && fieldValues.contains("true");

            Path currentFile = fileNavigator.getCurrentFile();
            String yamlContent = Files.readString(currentFile);
            String updatedYaml = updateOptionalBooleanInYaml(yamlContent, fieldName, enabled);
            Files.writeString(currentFile, updatedYaml);

            ctx.redirect("/");
        } catch (Exception e) {
            ctx.html(renderError("Update error", e.getMessage()));
        }
    }

    private static void handleUpdateOptionalRankField(Context ctx, String fieldName, String formFieldName) {
        try {
            String value = ctx.formParam(formFieldName);
            Integer rank = null;

            if (value != null && !value.trim().isEmpty()) {
                rank = Integer.parseInt(value.trim());
            }

            Path currentFile = fileNavigator.getCurrentFile();
            String yamlContent = Files.readString(currentFile);
            String updatedYaml = updateOptionalRankInYaml(yamlContent, fieldName, rank);
            Files.writeString(currentFile, updatedYaml);

            ctx.redirect("/");
        } catch (NumberFormatException e) {
            ctx.html(renderError("Update error", "Please enter a whole number or leave blank to clear."));
        } catch (Exception e) {
            ctx.html(renderError("Update error", e.getMessage()));
        }
    }

    private static void handleUpdateAddress(Context ctx) {
        try {
            String newAddress = ctx.formParam("newAddress");
            if (newAddress == null) {
                newAddress = "";
            }
            newAddress = newAddress.trim();

            Path currentFile = fileNavigator.getCurrentFile();
            String yamlContent = Files.readString(currentFile);

            String updatedYaml = updateAddressInYaml(yamlContent, newAddress);

            Files.writeString(currentFile, updatedYaml);

            ctx.redirect("/");
        } catch (Exception e) {
            ctx.html(renderError("Update error", e.getMessage()));
        }
    }

    private static void handleUpdateStayImage(Context ctx) {
        try {
            String newStayImageUrl = ctx.formParam("newStayImageUrl");
            if (newStayImageUrl == null) {
                newStayImageUrl = "";
            }
            newStayImageUrl = newStayImageUrl.trim();

            Path currentFile = fileNavigator.getCurrentFile();
            String yamlContent = Files.readString(currentFile);

            String updatedYaml = updateStayImageUrlInYaml(yamlContent, newStayImageUrl);

            Files.writeString(currentFile, updatedYaml);

            ctx.redirect("/");
        } catch (Exception e) {
            ctx.html(renderError("Update error", e.getMessage()));
        }
    }
    
    private static void handleDownloadImage(Context ctx) {
        try {
            String imageUrl = ctx.formParam("newImageUrl");
            if (imageUrl == null || imageUrl.isEmpty()) {
                ctx.html(renderError("Download error", "Please provide an image URL"));
                return;
            }
            
            Path currentFile = fileNavigator.getCurrentFile();
            String yamlContent = Files.readString(currentFile);
            
            // Download the image
            DownloadResult result = downloadAndSaveImage(imageUrl, currentFile);
            
            if (!result.success) {
                ctx.html(renderError("Download error", result.message));
                return;
            }
            
            // Update YAML content with new image path
            String updatedYaml = updateImageUrlInYaml(yamlContent, result.localImagePath);
            
            // Save the updated YAML
            Files.writeString(currentFile, updatedYaml);
            
            // Redirect to reload the page
            ctx.redirect("/");
        } catch (Exception e) {
            ctx.html(renderError("Download error", e.getMessage()));
        }
    }

    private static void handleGenerateThumbnails(Context ctx) {
        int generated = 0;
        int skipped = 0;
        int failed = 0;
        List<String> warnings = new ArrayList<>();

        try {
            Path imagesDir = Paths.get(IMAGES_PATH);
            if (!Files.exists(imagesDir)) {
                ctx.html(renderError("Thumbnail generation error", "Images directory not found: " + imagesDir));
                return;
            }

            List<Path> imageFiles;
            try (var stream = Files.list(imagesDir)) {
                imageFiles = stream
                        .filter(Files::isRegularFile)
                        .filter(path -> {
                            String name = path.getFileName().toString().toLowerCase();
                            return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp") || name.endsWith(".gif");
                        })
                        .sorted()
                        .toList();
            }

            for (Path sourcePath : imageFiles) {
                String fileName = sourcePath.getFileName().toString();
                int dot = fileName.lastIndexOf('.');
                String baseName = dot > 0 ? fileName.substring(0, dot) : fileName;
                Path targetPath = Paths.get(THUMBS_PATH, baseName + ".jpg");

                if (Files.exists(targetPath)) {
                    skipped++;
                    continue;
                }

                try {
                    generateThumbnail(sourcePath, targetPath, THUMB_WIDTH);
                    generated++;
                } catch (Exception e) {
                    failed++;
                    warnings.add(fileName + ": " + e.getMessage());
                }
            }

            StringBuilder summary = new StringBuilder();
            summary.append("✅ Thumbnail generation complete: ")
                    .append(generated).append(" generated, ")
                    .append(skipped).append(" already existed, ")
                    .append(failed).append(" failed.");

            if (!warnings.isEmpty()) {
                summary.append("<br><br>⚠️ Warnings:<br>");
                for (String warning : warnings) {
                    summary.append("• ").append(escapeHtml(warning)).append("<br>");
                }
            }

            Path currentFile = fileNavigator.getCurrentFile();
            String yamlContent = Files.readString(currentFile);
            String fileName = currentFile.getFileName().toString();
            CourseData course = parseCourse(yamlContent);
            ValidationResult websiteValidation = validator.validateWebsite(course.website);
            ValidationResult imageValidation = validator.validateImage(course.mainImageUrl);
            ValidationResult stayImageValidation = validator.validateImage(course.stayImageUrl);
            ctx.html(renderHTML(fileName, yamlContent, course, websiteValidation, imageValidation, stayImageValidation, summary.toString()));
        } catch (Exception e) {
            ctx.html(renderError("Thumbnail generation error", e.getMessage()));
        }
    }
    
    private static void handleJumpToLetter(Context ctx) {
        try {
            String letter = ctx.formParam("letter");
            if (letter == null || letter.isEmpty()) {
                ctx.redirect("/");
                return;
            }
            
            int index = fileNavigator.findFirstCourseStartingWith(letter.charAt(0));
            if (index >= 0) {
                fileNavigator.jumpToIndex(index);
            }
            ctx.redirect("/");
        } catch (Exception e) {
            ctx.html(renderError("Navigation error", e.getMessage()));
        }
    }
    
    private static void handleSearchCourse(Context ctx) {
        try {
            String searchTerm = ctx.formParam("search");
            if (searchTerm == null || searchTerm.isEmpty()) {
                ctx.redirect("/");
                return;
            }
            
            int index = fileNavigator.findCourseByName(searchTerm);
            if (index >= 0) {
                fileNavigator.jumpToIndex(index);
            }
            ctx.redirect("/");
        } catch (Exception e) {
            ctx.html(renderError("Search error", e.getMessage()));
        }
    }
    
    private static void handleCoursesList(Context ctx) {
        try {
            List<Map<String, Object>> courses = fileNavigator.getAllCoursesInfo();
            ctx.json(courses);
        } catch (Exception e) {
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }

    private static void handleGeocodeCurrent(Context ctx) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(GEOCODE_CONNECT_TIMEOUT)
                .build();
        ObjectMapper jsonMapper = new ObjectMapper();

        try {
            Path currentFile = fileNavigator.getCurrentFile();
            String yamlContent = Files.readString(currentFile);
            String fileName = currentFile.getFileName().toString();
            CourseData course = parseCourse(yamlContent);

            log.info("[geocode-current] start file={} course='{}' address='{}' existingLatLng={}",
                    fileName,
                    course.name(),
                    truncateForLog(course.address(), 160),
                    course.lat() != null && course.lng() != null);

            String message;
            Double existingLat = course.lat();
            Double existingLng = course.lng();
            if (course.address() == null || course.address().isBlank()) {
                message = "⚠️ Cannot geocode: this course has no address.";
                log.warn("[geocode-current] skipped file={} reason=no_address", fileName);
            } else {
                Optional<LatLng> maybeLatLng = geocodeWithFallback(httpClient, jsonMapper, fileName, course.name(), course.address(), "[geocode-current]");

                if (maybeLatLng.isEmpty()) {
                    message = "⚠️ No geocoding result found for this course address.";
                    log.warn("[geocode-current] no-results file={} address='{}'", fileName, truncateForLog(course.address(), 160));
                } else {
                    LatLng latLng = maybeLatLng.get();
                    double lat = latLng.lat();
                    double lng = latLng.lng();
                    log.info("[geocode-current] parsed file={} lat={} lng={}", fileName, lat, lng);
                    String updatedYaml = updateLatLngInYaml(yamlContent, lat, lng);
                    Files.writeString(currentFile, updatedYaml);
                    log.info("[geocode-current] wrote file={} lat={} lng={}", fileName, lat, lng);
                    yamlContent = updatedYaml;
                    course = parseCourse(yamlContent);
                    if (existingLat != null && existingLng != null) {
                        message = "✅ Geocoded current course and replaced coordinates: " + existingLat + ", " + existingLng + " → " + lat + ", " + lng;
                    } else {
                        message = "✅ Geocoded current course: " + lat + ", " + lng;
                    }
                }
            }

            ValidationResult websiteValidation = validator.validateWebsite(course.website);
            ValidationResult imageValidation = validator.validateImage(course.mainImageUrl);
            ValidationResult stayImageValidation = validator.validateImage(course.stayImageUrl);
            ctx.html(renderHTML(fileName, yamlContent, course, websiteValidation, imageValidation, stayImageValidation, message));
        } catch (Exception e) {
            log.error("[geocode-current] error", e);
            ctx.html(renderError("Geocoding error", e.getMessage()));
        }
    }

    private static void handleGeocodeAll(Context ctx) {
        Path coursesDir = Paths.get(COURSES_PATH);
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(GEOCODE_CONNECT_TIMEOUT)
                .build();
        ObjectMapper jsonMapper = new ObjectMapper();

        int skipped = 0;
        int geocoded = 0;
        int failed = 0;
        int noAddress = 0;
        List<String> warnings = new ArrayList<>();

        try {
            List<Path> courseFiles = Files.list(coursesDir)
                    .filter(p -> p.toString().endsWith(".yaml"))
                    .sorted()
                    .collect(Collectors.toList());

            for (Path courseFile : courseFiles) {
                String yamlContent = Files.readString(courseFile);
                CourseData course;
                try {
                    course = parseCourse(yamlContent);
                } catch (Exception e) {
                    warnings.add("Parse error for " + courseFile.getFileName() + ": " + e.getMessage());
                    failed++;
                    continue;
                }

                // Skip if already has lat/lng
                if (course.lat() != null && course.lng() != null) {
                    skipped++;
                    continue;
                }

                // Skip if no address
                if (course.address() == null || course.address().isBlank()) {
                    noAddress++;
                    continue;
                }

                // Respect Nominatim 1 req/sec rate limit
                try {
                    Thread.sleep(1100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    warnings.add("Geocoding interrupted");
                    break;
                }

                // Call Nominatim
                try {
                    Optional<LatLng> maybeLatLng = geocodeWithFallback(
                        httpClient,
                        jsonMapper,
                        courseFile.getFileName().toString(),
                        course.name(),
                        course.address(),
                        "[geocode-all]");

                    if (maybeLatLng.isEmpty()) {
                        warnings.add("No geocoding result for '" + course.name() + "' (address: " + course.address() + ")");
                        log.warn("[geocode-all] no-results file={} course='{}'", courseFile.getFileName(), course.name());
                        failed++;
                        continue;
                    }

                    LatLng latLng = maybeLatLng.get();
                    double lat = latLng.lat();
                    double lng = latLng.lng();
                    log.info("[geocode-all] parsed file={} course='{}' lat={} lng={}", courseFile.getFileName(), course.name(), lat, lng);

                    // Write lat/lng back to YAML file
                    String updatedYaml = updateLatLngInYaml(yamlContent, lat, lng);
                    Files.writeString(courseFile, updatedYaml);
                    log.info("[geocode-all] wrote file={} lat={} lng={}", courseFile.getFileName(), lat, lng);
                    geocoded++;

                } catch (Exception e) {
                    warnings.add("Geocoding failed for '" + course.name() + "': " + e.getMessage());
                    log.error("[geocode-all] error file={} course='{}'", courseFile.getFileName(), course.name(), e);
                    failed++;
                }
            }

        } catch (Exception e) {
            ctx.html(renderError("Geocoding error", e.getMessage()));
            return;
        }

        // Build summary message
        StringBuilder summary = new StringBuilder();
        summary.append("✅ Geocoding complete: ")
               .append(geocoded).append(" geocoded, ")
               .append(skipped).append(" already had coordinates, ")
               .append(noAddress).append(" had no address, ")
               .append(failed).append(" failed.");
        if (!warnings.isEmpty()) {
            summary.append("<br><br>⚠️ Warnings:<br>");
            for (String w : warnings) {
                summary.append("• ").append(escapeHtml(w)).append("<br>");
            }
        }

        // Re-render current course page with summary
        try {
            Path currentFile = fileNavigator.getCurrentFile();
            String yamlContent = Files.readString(currentFile);
            String fileName = currentFile.getFileName().toString();
            CourseData course = parseCourse(yamlContent);
            ValidationResult websiteValidation = validator.validateWebsite(course.website);
            ValidationResult imageValidation = validator.validateImage(course.mainImageUrl);
            ValidationResult stayImageValidation = validator.validateImage(course.stayImageUrl);
            ctx.html(renderHTML(fileName, yamlContent, course, websiteValidation, imageValidation, stayImageValidation, summary.toString()));
        } catch (Exception e) {
            ctx.html(renderError("Geocoding complete but page render failed", e.getMessage()));
        }
    }

    private record LatLng(double lat, double lng) {}

    private static Optional<LatLng> geocodeWithFallback(
            HttpClient httpClient,
            ObjectMapper jsonMapper,
            String fileName,
            String courseName,
            String address,
            String logPrefix) throws IOException, InterruptedException {

        String normalizedAddress = address == null ? "" : address.trim();
        String cleanedAddress = normalizedAddress
                .replaceAll("(?i)^hire\\s+", "")
                .replaceAll("\\s+", " ")
                .trim();
        String normalizedCourseName = courseName == null ? "" : courseName
                .replaceAll("(?i)\\s+previously\\b.*$", "")
                .replaceAll("\\s+", " ")
                .trim();

        LinkedHashSet<String> queryVariants = new LinkedHashSet<>();
        if (!normalizedAddress.isBlank()) {
            queryVariants.add(normalizedAddress);
            queryVariants.add(normalizedAddress + ", United Kingdom");
        }
        if (!cleanedAddress.isBlank()) {
            queryVariants.add(cleanedAddress);
            queryVariants.add(cleanedAddress + ", United Kingdom");
        }
        if (!normalizedCourseName.isBlank() && !cleanedAddress.isBlank()) {
            queryVariants.add(normalizedCourseName + ", " + cleanedAddress + ", United Kingdom");
        }

        Optional<String> postcode = extractUkPostcode(normalizedAddress);
        postcode.ifPresent(pc -> {
            queryVariants.add(pc);
            queryVariants.add(pc + ", United Kingdom");
            if (!normalizedCourseName.isBlank()) {
                queryVariants.add(normalizedCourseName + ", " + pc + ", United Kingdom");
            }
        });

        int attempt = 0;
        for (String query : queryVariants) {
            for (boolean useCountryCodeFilter : new boolean[]{true, false}) {
                attempt++;
                String encodedQuery = java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
                String nominatimUrl = "https://nominatim.openstreetmap.org/search?q=" + encodedQuery + "&format=json&limit=1";
                if (useCountryCodeFilter) {
                    nominatimUrl += "&countrycodes=gb";
                }

                log.info("{} attempt={} file={} countryFilter={} query='{}' url={}",
                        logPrefix,
                        attempt,
                        fileName,
                        useCountryCodeFilter,
                        truncateForLog(query, 180),
                        nominatimUrl);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(nominatimUrl))
                        .timeout(GEOCODE_REQUEST_TIMEOUT)
                        .header("User-Agent", "YorkshireGolfCourseAudit/1.0")
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                String body = response.body();

                log.info("{} attempt={} file={} status={} bodyPreview={}",
                        logPrefix,
                        attempt,
                        fileName,
                        response.statusCode(),
                        truncateForLog(body, 500));

                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    continue;
                }

                JsonNode results = jsonMapper.readTree(body);
                if (!results.isArray() || results.isEmpty()) {
                    continue;
                }

                JsonNode first = results.get(0);
                JsonNode latNode = first.get("lat");
                JsonNode lonNode = first.get("lon");
                if (latNode == null || lonNode == null) {
                    continue;
                }

                return Optional.of(new LatLng(latNode.asDouble(), lonNode.asDouble()));
            }
        }

        return Optional.empty();
    }

    private static Optional<String> extractUkPostcode(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?i)\\b([A-Z]{1,2}\\d[A-Z\\d]?\\s*\\d[A-Z]{2})\\b");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String postcode = matcher.group(1).toUpperCase().replaceAll("\\s+", "");
        if (postcode.length() > 3) {
            postcode = postcode.substring(0, postcode.length() - 3) + " " + postcode.substring(postcode.length() - 3);
        }
        return Optional.of(postcode);
    }

    private static String truncateForLog(String value, int maxLength) {
        if (value == null) {
            return "null";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
    
    private static void handleComputeNearby(Context ctx) {
        Path coursesDir = Paths.get(COURSES_PATH);
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

        // Build list of all courses that have lat/lng
        record GeoEntry(Path file, String name, double lat, double lng) {}
        List<GeoEntry> geoEntries = new ArrayList<>();
        int noCoords = 0;
        List<String> warnings = new ArrayList<>();

        try {
            List<Path> courseFiles = Files.list(coursesDir)
                    .filter(p -> p.toString().endsWith(".yaml"))
                    .sorted()
                    .collect(Collectors.toList());

            for (Path file : courseFiles) {
                String yamlContent = Files.readString(file);
                CourseData course;
                try {
                    course = parseCourse(yamlContent);
                } catch (Exception e) {
                    warnings.add("Parse error for " + file.getFileName() + ": " + e.getMessage());
                    continue;
                }
                if (course.lat() == null || course.lng() == null) {
                    noCoords++;
                    continue;
                }
                geoEntries.add(new GeoEntry(file, course.name(), course.lat(), course.lng()));
            }

            // For each geo-entry, compute the 3 nearest and write back to YAML
            int computed = 0;
            for (GeoEntry entry : geoEntries) {
                List<String> nearest = geoEntries.stream()
                        .filter(other -> !other.file().equals(entry.file()))
                        .sorted(java.util.Comparator.comparingDouble(
                                other -> haversineKm(entry.lat(), entry.lng(), other.lat(), other.lng())))
                        .limit(3)
                        .map(GeoEntry::name)
                        .collect(Collectors.toList());

                String nearby1 = !nearest.isEmpty() ? nearest.get(0) : null;
                String nearby2 = nearest.size() >= 2 ? nearest.get(1) : null;
                String nearby3 = nearest.size() >= 3 ? nearest.get(2) : null;

                try {
                    String yamlContent = Files.readString(entry.file());
                    String updated = updateNearbyInYaml(yamlContent, nearby1, nearby2, nearby3);
                    Files.writeString(entry.file(), updated);
                    computed++;
                } catch (Exception e) {
                    warnings.add("Failed to write nearby for '" + entry.name() + "': " + e.getMessage());
                }
            }

            // Build summary
            StringBuilder summary = new StringBuilder();
            summary.append("✅ Nearby courses computed: ")
                   .append(computed).append(" updated, ")
                   .append(noCoords).append(" skipped (no lat/lng).");
            if (!warnings.isEmpty()) {
                summary.append("<br><br>⚠️ Warnings:<br>");
                for (String w : warnings) {
                    summary.append("• ").append(escapeHtml(w)).append("<br>");
                }
            }

            // Re-render current course page with summary
            Path currentFile = fileNavigator.getCurrentFile();
            String yamlContent = Files.readString(currentFile);
            String fileName = currentFile.getFileName().toString();
            CourseData course = parseCourse(yamlContent);
            ValidationResult websiteValidation = validator.validateWebsite(course.website);
            ValidationResult imageValidation = validator.validateImage(course.mainImageUrl);
            ValidationResult stayImageValidation = validator.validateImage(course.stayImageUrl);
            ctx.html(renderHTML(fileName, yamlContent, course, websiteValidation, imageValidation, stayImageValidation, summary.toString()));

        } catch (Exception e) {
            ctx.html(renderError("Nearby computation error", e.getMessage()));
        }
    }

    private static void handleComputeNearbyCurrent(Context ctx) {
        Path coursesDir = Paths.get(COURSES_PATH);

        record GeoEntry(Path file, String name, double lat, double lng) {}
        List<GeoEntry> geoEntries = new ArrayList<>();

        try {
            Path currentFile = fileNavigator.getCurrentFile();
            String currentYaml = Files.readString(currentFile);
            String fileName = currentFile.getFileName().toString();
            CourseData currentCourse = parseCourse(currentYaml);

            if (currentCourse.lat() == null || currentCourse.lng() == null) {
                ValidationResult websiteValidation = validator.validateWebsite(currentCourse.website);
                ValidationResult imageValidation = validator.validateImage(currentCourse.mainImageUrl);
                ValidationResult stayImageValidation = validator.validateImage(currentCourse.stayImageUrl);
                ctx.html(renderHTML(fileName, currentYaml, currentCourse, websiteValidation, imageValidation, stayImageValidation,
                        "⚠️ Cannot compute nearby: current course has no lat/lng."));
                return;
            }

            List<Path> courseFiles = Files.list(coursesDir)
                    .filter(p -> p.toString().endsWith(".yaml"))
                    .sorted()
                    .collect(Collectors.toList());

            for (Path file : courseFiles) {
                String yamlContent = Files.readString(file);
                CourseData course;
                try {
                    course = parseCourse(yamlContent);
                } catch (Exception ignored) {
                    continue;
                }
                if (course.lat() == null || course.lng() == null) {
                    continue;
                }
                geoEntries.add(new GeoEntry(file, course.name(), course.lat(), course.lng()));
            }

            List<String> nearest = geoEntries.stream()
                    .filter(other -> !other.file().equals(currentFile))
                    .sorted(java.util.Comparator.comparingDouble(
                            other -> haversineKm(currentCourse.lat(), currentCourse.lng(), other.lat(), other.lng())))
                    .limit(3)
                    .map(GeoEntry::name)
                    .collect(Collectors.toList());

            String nearby1 = !nearest.isEmpty() ? nearest.get(0) : null;
            String nearby2 = nearest.size() >= 2 ? nearest.get(1) : null;
            String nearby3 = nearest.size() >= 3 ? nearest.get(2) : null;

            String updated = updateNearbyInYaml(currentYaml, nearby1, nearby2, nearby3);
            Files.writeString(currentFile, updated);

            CourseData updatedCourse = parseCourse(updated);
            ValidationResult websiteValidation = validator.validateWebsite(updatedCourse.website);
            ValidationResult imageValidation = validator.validateImage(updatedCourse.mainImageUrl);
            ValidationResult stayImageValidation = validator.validateImage(updatedCourse.stayImageUrl);
            ctx.html(renderHTML(fileName, updated, updatedCourse, websiteValidation, imageValidation, stayImageValidation,
                    "✅ Nearby courses updated for current course only."));
        } catch (Exception e) {
            ctx.html(renderError("Nearby computation error", e.getMessage()));
        }
    }

    private static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
    
    private static CourseData parseCourse(String yaml) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Map<String, Object> data = mapper.readValue(yaml, Map.class);
        
        String name = (String) data.get("name");
        String website = (String) data.get("website");
        String golfnowUrl = (String) data.get("golfnowUrl");
        String mainImageUrl = (String) data.get("mainImageUrl");
        String stayImageUrl = (String) data.get("stayImageUrl");
        
        // Parse closed field - handle both Boolean and String types
        boolean closed = false;
        Object closedObj = data.get("closed");
        if (closedObj instanceof Boolean) {
            closed = (Boolean) closedObj;
        } else if (closedObj instanceof String) {
            closed = "true".equalsIgnoreCase((String) closedObj);
        }

        boolean playAndStay = false;
        Object playAndStayObj = data.get("playAndStay");
        if (playAndStayObj instanceof Boolean) {
            playAndStay = (Boolean) playAndStayObj;
        } else if (playAndStayObj instanceof String) {
            playAndStay = "true".equalsIgnoreCase((String) playAndStayObj);
        }
        
        // Extract region.name
        String region = null;
        Object regionObj = data.get("region");
        if (regionObj instanceof Map) {
            region = (String) ((Map<?, ?>) regionObj).get("name");
        }

        String address = (String) data.get("address");

        // Parse lat/lng fields
        Double lat = null;
        Double lng = null;
        Object latObj = data.get("lat");
        Object lngObj = data.get("lng");
        if (latObj instanceof Number) {
            lat = ((Number) latObj).doubleValue();
        } else if (latObj instanceof String) {
            try { lat = Double.parseDouble((String) latObj); } catch (NumberFormatException ignored) {}
        }
        if (lngObj instanceof Number) {
            lng = ((Number) lngObj).doubleValue();
        } else if (lngObj instanceof String) {
            try { lng = Double.parseDouble((String) lngObj); } catch (NumberFormatException ignored) {}
        }

        // Parse nearby fields
        String nearby1 = (String) data.get("nearby1");
        String nearby2 = (String) data.get("nearby2");
        String nearby3 = (String) data.get("nearby3");

        Integer top100 = null;
        Object top100Obj = data.get("top100");
        if (top100Obj instanceof Integer) {
            top100 = (Integer) top100Obj;
        } else if (top100Obj instanceof String) {
            try {
                top100 = Integer.parseInt((String) top100Obj);
            } catch (NumberFormatException ignored) {}
        }
        
        // Parse next100 field. Accept legacy numeric/string values as true.
        Boolean next100 = null;
        Object next100Obj = data.get("next100");
        if (next100Obj instanceof Boolean) {
            next100 = (Boolean) next100Obj;
        } else if (next100Obj instanceof Number) {
            next100 = ((Number) next100Obj).intValue() > 0;
        } else if (next100Obj instanceof String) {
            String value = ((String) next100Obj).trim();
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                next100 = Boolean.parseBoolean(value);
            } else {
                try {
                    next100 = Integer.parseInt(value) > 0;
                } catch (NumberFormatException ignored) {}
            }
        }
        
        return new CourseData(name, website, golfnowUrl, mainImageUrl, stayImageUrl, region, closed, playAndStay, address, lat, lng, nearby1, nearby2, nearby3, top100, next100);
    }
    
    private static String renderHTML(String fileName, String yamlContent, CourseData course,
                                     ValidationResult websiteValidation, ValidationResult imageValidation,
                                     ValidationResult stayImageValidation,
                                     String message) {
        int current = fileNavigator.getCurrentIndex() + 1;
        int total = fileNavigator.getTotalFiles();
        boolean hasNext = fileNavigator.hasNext();
        boolean hasPrev = fileNavigator.hasPrevious();
        
        // Image preview
        String imagePreview = renderImagePreview(course.mainImageUrl, imageValidation);
        String stayImagePreview = renderImagePreview(course.stayImageUrl, stayImageValidation);
        
        // Website validation with visit button
        String websiteSection = renderWebsiteValidation(course.website, websiteValidation);
        
        // GolfNow URL section
        String golfnowSection = renderGolfNowUrlSection(course.name, course.golfnowUrl);
        
        // Navigation header with alphabet and search
        String navHeader = renderNavigationHeader();
        
        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Course Audit - %s</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { 
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            padding: 20px;
            background: #f5f5f5;
        }
        .container { 
            max-width: 1400px;
            margin: 0 auto;
            background: white;
            padding: 30px;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        .container.closed-course {
            border: 4px solid #ef5350;
        }
        .header {
            border-bottom: 2px solid #e0e0e0;
            padding-bottom: 20px;
            margin-bottom: 20px;
        }
        h1 { 
            color: #333;
            font-size: 24px;
            margin-bottom: 10px;
        }
        .progress {
            color: #666;
            font-size: 14px;
            margin-top: 5px;
        }
        .content-area {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 30px;
            margin-bottom: 20px;
        }
        .left-panel, .right-panel {
            display: flex;
            flex-direction: column;
        }
        .image-section {
            margin-bottom: 20px;
        }
        .image-preview {
            text-align: center;
            padding: 20px;
            background: #fafafa;
            border: 1px solid #e0e0e0;
            border-radius: 4px;
            position: relative;
        }
        .image-preview img {
            max-width: 100%%;
            max-height: 400px;
            display: block;
            margin: 0 auto 10px auto;
            border-radius: 4px;
        }
        .image-preview.placeholder img {
            border: 3px solid #ff9800;
        }
        .image-preview.error {
            background: #ffebee;
            border-color: #ef5350;
        }
        .image-dimensions {
            color: #666;
            font-size: 14px;
            margin-top: 10px;
        }
        .image-warning {
            display: inline-block;
            background: #ff9800;
            color: white;
            padding: 4px 12px;
            border-radius: 4px;
            font-size: 12px;
            font-weight: bold;
            margin-top: 8px;
        }
        .image-error {
            display: inline-block;
            background: #ef5350;
            color: white;
            padding: 4px 12px;
            border-radius: 4px;
            font-size: 12px;
            font-weight: bold;
            margin-top: 8px;
        }
        .validation-section {
            margin-bottom: 20px;
        }
        .validation-label {
            font-weight: 600;
            color: #333;
            margin-bottom: 8px;
            font-size: 14px;
        }
        .validation-result {
            padding: 12px;
            border-radius: 4px;
            font-size: 14px;
            margin-bottom: 10px;
        }
        .validation-result.ok {
            background: #e8f5e9;
            border: 1px solid #66bb6a;
            color: #2e7d32;
        }
        .validation-result.warning {
            background: #fff3e0;
            border: 1px solid #ffa726;
            color: #e65100;
        }
        .validation-result.error {
            background: #ffebee;
            border: 1px solid #ef5350;
            color: #c62828;
        }
        .validation-result.neutral {
            background: #f5f5f5;
            border: 1px solid #bdbdbd;
            color: #616161;
        }
        .form-input {
            width: 100%%;
            padding: 10px;
            border: 1px solid #ccc;
            border-radius: 4px;
            font-size: 14px;
            margin-bottom: 10px;
        }
        .form-section {
            background: #f5f5f5;
            padding: 15px;
            border-radius: 4px;
            border: 1px solid #e0e0e0;
            margin-bottom: 15px;
        }
        .form-button {
            width: 100%%;
            padding: 12px;
            font-size: 14px;
        }
        .button-row {
            display: flex;
            gap: 10px;
            justify-content: space-between;
            padding-top: 20px;
            border-top: 2px solid #e0e0e0;
        }
        .button-group {
            display: flex;
            gap: 10px;
        }
        button {
            padding: 12px 24px;
            border: none;
            border-radius: 4px;
            font-size: 14px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.2s;
        }
        button:hover:not(:disabled) {
            transform: translateY(-1px);
            box-shadow: 0 2px 8px rgba(0,0,0,0.15);
        }
        button:disabled {
            opacity: 0.5;
            cursor: not-allowed;
        }
        .btn-primary {
            background: #2196f3;
            color: white;
        }
        .btn-success {
            background: #4caf50;
            color: white;
        }
        .btn-secondary {
            background: #757575;
            color: white;
        }
        .btn-link {
            background: #2196f3;
            color: white;
            text-decoration: none;
            display: inline-block;
            padding: 8px 16px;
            border-radius: 4px;
            font-size: 13px;
            font-weight: 600;
            margin-top: 8px;
        }
        .btn-link:hover {
            background: #1976d2;
            transform: translateY(-1px);
            box-shadow: 0 2px 8px rgba(0,0,0,0.15);
        }
        .btn-link:disabled {
            background: #bdbdbd;
            cursor: not-allowed;
            opacity: 0.6;
        }
        .success-message, .error-message, .info-message {
            padding: 12px 20px;
            border-radius: 4px;
            margin-bottom: 20px;
            font-weight: 500;
        }
        .success-message {
            background: #e8f5e9;
            color: #2e7d32;
            border: 1px solid #66bb6a;
        }
        .error-message {
            background: #ffebee;
            color: #c62828;
            border: 1px solid #ef5350;
        }
        .info-message {
            background: #e3f2fd;
            color: #1565c0;
            border: 1px solid #42a5f5;
        }
        .closed-badge {
            display: inline-block;
            background: #ef5350;
            color: white;
            padding: 6px 16px;
            border-radius: 4px;
            font-size: 14px;
            font-weight: bold;
            margin-left: 15px;
            vertical-align: middle;
        }
        .nav-header {
            background: #f8f9fa;
            padding: 15px 20px;
            border-radius: 6px;
            margin-bottom: 20px;
            border: 1px solid #e0e0e0;
        }
        .top-actions {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 12px;
            background: #eef6ff;
            border: 1px solid #bbdefb;
            border-radius: 6px;
            padding: 12px 16px;
            margin-bottom: 20px;
        }
        .top-actions__title {
            font-size: 13px;
            font-weight: 700;
            color: #0d47a1;
            text-transform: uppercase;
            letter-spacing: 0.4px;
        }
        .top-actions__hint {
            font-size: 13px;
            color: #0d47a1;
        }
        .nav-section {
            margin-bottom: 12px;
        }
        .nav-section:last-child {
            margin-bottom: 0;
        }
        .nav-label {
            font-size: 12px;
            font-weight: 600;
            color: #666;
            margin-bottom: 6px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }
        .alphabet-nav {
            display: flex;
            flex-wrap: wrap;
            gap: 6px;
        }
        .letter-btn {
            background: white;
            border: 1px solid #ccc;
            padding: 6px 10px;
            border-radius: 4px;
            font-size: 13px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.2s;
            min-width: 32px;
            text-align: center;
        }
        .letter-btn:hover {
            background: #2196f3;
            color: white;
            border-color: #2196f3;
            transform: translateY(-1px);
        }
        .letter-btn.active {
            background: #1976d2;
            color: white;
            border-color: #1976d2;
        }
        .letter-btn:disabled {
            background: #f5f5f5;
            color: #ccc;
            cursor: not-allowed;
            border-color: #e0e0e0;
        }
        .letter-btn:disabled:hover {
            transform: none;
        }
        .search-box {
            display: flex;
            gap: 10px;
        }
        .search-input {
            flex: 1;
            padding: 10px 15px;
            border: 1px solid #ccc;
            border-radius: 4px;
            font-size: 14px;
        }
        .search-input:focus {
            outline: none;
            border-color: #2196f3;
            box-shadow: 0 0 0 3px rgba(33, 150, 243, 0.1);
        }
        .search-btn {
            background: #2196f3;
            color: white;
            border: none;
            padding: 10px 20px;
            border-radius: 4px;
            font-size: 14px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.2s;
        }
        .search-btn:hover {
            background: #1976d2;
            transform: translateY(-1px);
        }
        .search-results {
            position: absolute;
            background: white;
            border: 1px solid #ccc;
            border-radius: 4px;
            max-height: 300px;
            overflow-y: auto;
            z-index: 1000;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            display: none;
        }
        .search-result-item {
            padding: 10px 15px;
            cursor: pointer;
            border-bottom: 1px solid #f0f0f0;
        }
        .search-result-item:hover {
            background: #f5f5f5;
        }
        .search-result-item:last-child {
            border-bottom: none;
        }
    </style>
</head>
<body>
    <div class="container%s">
        <div class="header">
            <h1>Course Audit: %s%s</h1>
            <div class="progress">File %d of %d</div>
        </div>
        
        %s
        
        %s

        <div class="top-actions">
            <div>
                <div class="top-actions__title">Image Tools</div>
                <div class="top-actions__hint">Generate thumbnails for all course images (writes missing files only).</div>
            </div>
            <form method="post" action="/generate-thumbnails">
                <button type="submit" formnovalidate class="btn-primary">🖼️ Generate All Missing Thumbnails</button>
            </form>
        </div>
        
        <form method="post">
            <div class="content-area">
                <div class="left-panel">
                    <div class="image-section">
                        <div class="validation-label">Image Preview</div>
                        %s
                    </div>

                    <div class="image-section">
                        <div class="validation-label">Stay Image Preview</div>
                        %s
                    </div>
                    
                    <div class="validation-section">
                        <div class="validation-label">Website Validation</div>
                        %s
                    </div>
                    
                    <div class="validation-section">
                        <div class="validation-label">Image Validation</div>
                        %s
                    </div>

                    <div class="validation-section">
                        <div class="validation-label">Stay Image Validation</div>
                        %s
                    </div>
                </div>
                
                <div class="right-panel">
                    <div class="validation-section">
                        <div class="validation-label">Update Website URL</div>
                        <div class="form-section">
                            <input type="url" name="newWebsiteUrl" placeholder="https://example.com" value="%s" class="form-input">
                            <button type="submit" formaction="/update-website" class="btn-primary form-button">🌐 Update Website</button>
                            <div style="font-size: 11px; color: #666; margin-top: 8px;">Updates website URL in file automatically</div>
                        </div>
                    </div>

                    %%GOLFNOW_SECTION%%

                    <div class="validation-section">
                        <div class="validation-label">Update Stay Image URL</div>
                        <div class="form-section">
                            <input type="url" name="newStayImageUrl" placeholder="https://example.com/stay-image.jpg" value="%s" class="form-input">
                            <button type="submit" formaction="/update-stay-image" class="btn-primary form-button">🏨 Update Stay Image URL</button>
                            <div style="font-size: 11px; color: #666; margin-top: 8px;">Updates stayImageUrl in file automatically</div>
                        </div>
                    </div>

                    <div class="validation-section">
                        <div class="validation-label">Update Address</div>
                        <div class="form-section">
                            <input type="text" name="newAddress" placeholder="e.g. Abbeydale Road South, Sheffield, S17 3LA" value="%s" class="form-input">
                            <button type="submit" formaction="/update-address" class="btn-primary form-button">📍 Update Address</button>
                            <div style="font-size: 11px; color: #666; margin-top: 8px;">Updates address in file automatically</div>
                        </div>
                    </div>

                    <div class="validation-section">
                        <div class="validation-label">Top 100 Rank (Optional)</div>
                        <div class="form-section">
                            <input type="number" name="newTop100" min="1" step="1" placeholder="e.g. 42" value="%s" class="form-input">
                            <button type="submit" formaction="/update-top100" class="btn-primary form-button">🏆 Update Top 100</button>
                            <div style="font-size: 11px; color: #666; margin-top: 8px;">Leave blank to remove top100 from YAML</div>
                        </div>
                    </div>

                    <div class="validation-section">
                        <div class="validation-label">Next 100 Membership</div>
                        <div class="form-section">
                            <div style="margin-bottom: 10px;">
                                <label style="display: flex; align-items: center; cursor: pointer;">
                                    <input type="hidden" name="next100" value="false">
                                    <input type="checkbox" name="next100" value="true" %s
                                         onchange="this.form.noValidate=true; this.form.action='/update-next100'; this.form.submit();"
                                           style="width: 20px; height: 20px; margin-right: 10px; cursor: pointer;">
                                    <span style="font-size: 14px;"><strong>Included in Next 100</strong></span>
                                </label>
                            </div>
                            <div style="font-size: 11px; color: #666; margin-top: 8px;">Unchecked removes next100 from YAML</div>
                        </div>
                    </div>
                    
                    <div class="validation-section">
                        <div class="validation-label">Download New Image</div>
                        <div class="form-section">
                            <input type="url" name="newImageUrl" placeholder="https://example.com/image.jpg" class="form-input">
                            <button type="submit" formaction="/download-image" class="btn-primary form-button">⬇️ Download & Set Image</button>
                            <div style="font-size: 11px; color: #666; margin-top: 8px;">Downloads image and updates file automatically</div>
                        </div>
                    </div>

                    <div class="validation-section">
                        <div class="validation-label">Course Status</div>
                        <div class="form-section">
                            <div style="margin-bottom: 10px;">
                                <label style="display: flex; align-items: center; cursor: pointer;">
                                    <input type="hidden" name="closed" value="false">
                                    <input type="checkbox" name="closed" value="true" %s 
                                         onchange="this.form.noValidate=true; this.form.action='/update-closed'; this.form.submit();" 
                                           style="width: 20px; height: 20px; margin-right: 10px; cursor: pointer;">
                                    <span style="font-size: 14px;"><strong>Mark as CLOSED</strong></span>
                                </label>
                            </div>
                            <div style="margin-bottom: 10px;">
                                <label style="display: flex; align-items: center; cursor: pointer;">
                                    <input type="hidden" name="playAndStay" value="false">
                                    <input type="checkbox" name="playAndStay" value="true" %s
                                         onchange="this.form.noValidate=true; this.form.action='/update-play-and-stay'; this.form.submit();"
                                           style="width: 20px; height: 20px; margin-right: 10px; cursor: pointer;">
                                    <span style="font-size: 14px;"><strong>Play & Stay</strong></span>
                                </label>
                            </div>
                            <div style="font-size: 11px; color: #666;">Check this box if the course is permanently closed</div>
                        </div>
                    </div>
                    
                    <div class="validation-section">
                        <div class="validation-label">Course Information</div>
                        <div style="background: #f9f9f9; padding: 15px; border-radius: 4px; border: 1px solid #e0e0e0;">
                            <div style="margin-bottom: 10px;"><strong>Name:</strong> %s</div>
                            <div style="margin-bottom: 10px;"><strong>Region:</strong> %s</div>
                            <div style="margin-bottom: 10px;"><strong>Play & Stay:</strong> %s</div>
                            <div style="margin-bottom: 10px;"><strong>Address:</strong> %s</div>
                            <div style="margin-bottom: 10px;"><strong>Lat/Lng:</strong> %s</div>
                            <div style="margin-bottom: 10px;"><strong>Nearby 1:</strong> %s</div>
                            <div style="margin-bottom: 10px;"><strong>Nearby 2:</strong> %s</div>
                            <div style="margin-bottom: 10px;"><strong>Nearby 3:</strong> %s</div>
                            <div style="margin-bottom: 10px;"><strong>Top 100:</strong> %s</div>
                            <div style="margin-bottom: 10px;"><strong>Next 100:</strong> %s</div>
                            <div style="margin-bottom: 10px;"><strong>Status:</strong> <span style="color: %s; font-weight: bold;">%s</span></div>
                            <div><strong>File:</strong> <code style="background: #e0e0e0; padding: 2px 6px; border-radius: 3px; font-size: 12px;">%s</code></div>
                        </div>
                    </div>
                </div>
            </div>
            
            <div class="button-row">
                <div class="button-group">
                    <button type="submit" formaction="/previous" formnovalidate class="btn-secondary" %s>← Previous</button>
                </div>
                <div class="button-group">
                    <button type="submit" formaction="/geocode-current" formnovalidate class="btn-primary" onclick="this.textContent='⏳ Geocoding…'; const btn=this; setTimeout(() => { btn.disabled = true; }, 0);">🌍 Geocode Current Course (Overwrite Lat/Lng)</button>
                    <button type="submit" formaction="/compute-nearby-current" formnovalidate class="btn-primary" onclick="this.textContent='⏳ Computing…'; const btn=this; setTimeout(() => { btn.disabled = true; }, 0);" title="Run after Geocode Current Course to calculate the 3 nearest courses for this course">📍 Calculate Nearby (Current Course)</button>
                    <button type="submit" formaction="/next" formnovalidate class="btn-success" %s>Next →</button>
                </div>
            </div>
        </form>
    </div>
</body>
</html>
        """.formatted(
            fileName,
            course.closed ? " closed-course" : "",
            fileName,  // Add fileName again for h1
            course.closed ? "<span class='closed-badge'>⚠️ CLOSED</span>" : "",
            current, 
            total,
            message != null ? message : "",
            navHeader,  // Navigation header
            imagePreview,
            stayImagePreview,
            "%%WEBSITE_SECTION%%",
            renderValidationResult(imageValidation),
            renderValidationResult(stayImageValidation),
            escapeHtml(course.website != null ? course.website : ""),
            escapeHtml(course.stayImageUrl != null ? course.stayImageUrl : ""),
            escapeHtml(course.address != null ? course.address : ""),
            course.top100 != null ? String.valueOf(course.top100) : "",
            Boolean.TRUE.equals(course.next100) ? "checked" : "",
            course.closed ? "checked" : "",
            course.playAndStay ? "checked" : "",
            escapeHtml(course.name != null ? course.name : "N/A"),
            escapeHtml(course.region != null ? course.region : "N/A"),
            course.playAndStay ? "Yes" : "No",
            escapeHtml(course.address != null ? course.address : "—"),
            course.lat != null && course.lng != null
                ? course.lat + ", " + course.lng
                : "<span style='color:#999'>—</span>",
            course.nearby1 != null ? escapeHtml(course.nearby1) : "<span style='color:#999'>—</span>",
            course.nearby2 != null ? escapeHtml(course.nearby2) : "<span style='color:#999'>—</span>",
            course.nearby3 != null ? escapeHtml(course.nearby3) : "<span style='color:#999'>—</span>",
            course.top100 != null ? String.valueOf(course.top100) : "<span style='color:#999'>—</span>",
            Boolean.TRUE.equals(course.next100) ? "Yes" : "No",
            course.closed ? "#ef5350" : "#4caf50",
            course.closed ? "CLOSED" : "OPEN",
            fileName,
            hasPrev ? "" : "disabled",
            hasNext ? "" : "disabled"
        ).replace("%WEBSITE_SECTION%", websiteSection)
         .replace("%GOLFNOW_SECTION%", golfnowSection);
    }
    
    private static String renderImagePreview(String imageUrl, ValidationResult validation) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return """
            <div class="image-preview error">
                <div class="image-error">❌ NO IMAGE URL</div>
            </div>
            """;
        }
        
        boolean isPlaceholder = PLACEHOLDER_IMAGE.equals(imageUrl);
        String previewClass = isPlaceholder ? "image-preview placeholder" : "image-preview";
        
        // Get image source
        String imgSrc;
        if (imageUrl.startsWith("/images/courses/")) {
            imgSrc = "/image-proxy?path=" + imageUrl;
        } else if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            imgSrc = imageUrl;
        } else {
            return """
            <div class="image-preview error">
                <div class="image-error">❌ INVALID IMAGE PATH</div>
            </div>
            """;
        }
        
        String warningBadge = isPlaceholder ? "<div class='image-warning'>⚠️ PLACEHOLDER IMAGE</div>" : "";
        String dimensions = validation.dimensions != null 
            ? "<div class='image-dimensions'>" + validation.dimensions + "</div>"
            : "";
        
        return """
        <div class="%s">
            <img src="%s" alt="Course image" onerror="if(!this.parentElement.querySelector('.image-error')){this.style.display='none'; this.parentElement.innerHTML += '<div class=\\'image-error\\'>❌ IMAGE NOT FOUND</div>';}">
            %s
            %s
        </div>
        """.formatted(previewClass, imgSrc, dimensions, warningBadge);
    }
    
    private static String renderWebsiteValidation(String websiteUrl, ValidationResult validation) {
        String validationHtml = renderValidationResult(validation);
        
        if (websiteUrl != null && !websiteUrl.isEmpty() && 
            (websiteUrl.startsWith("http://") || websiteUrl.startsWith("https://"))) {
            String button = """
            <a href="%s" target="_blank" rel="noopener noreferrer" class="btn-link">
                🌐 Visit Website
            </a>
            """.formatted(escapeHtml(websiteUrl));
            return validationHtml + button;
        } else if (websiteUrl != null && !websiteUrl.isEmpty()) {
            String button = """
            <a href="#" onclick="return false;" class="btn-link" style="opacity:0.5; cursor:not-allowed;" title="Invalid URL format">
                🌐 Visit Website
            </a>
            """;
            return validationHtml + button;
        }
        
        return validationHtml;
    }
    
    private static String renderGolfNowUrlSection(String courseName, String golfnowUrl) {
        String section = """
        <div class="validation-section">
            <div class="validation-label">GolfNow URL</div>
            <div class="form-section">
                <input type="url" name="newGolfNowUrl" placeholder="https://www.golfnow.co.uk/courses/..." value="%s" class="form-input">
                <button type="submit" formaction="/update-golfnow-url" class="btn-primary form-button">🔗 Update GolfNow URL</button>
                <div style="font-size: 11px; color: #666; margin-top: 8px;">Updates golfnowUrl in file automatically</div>
            </div>
            
            <div style="margin-top: 12px; display: flex; flex-direction: column; gap: 8px;">
        """.formatted(golfnowUrl != null ? escapeHtml(golfnowUrl) : "");
        
        if (golfnowUrl != null && !golfnowUrl.isEmpty() && 
            (golfnowUrl.startsWith("http://") || golfnowUrl.startsWith("https://"))) {
            section += """
                <a href="%s" target="_blank" rel="noopener noreferrer" class="btn-link" style="text-align: center;">
                    ✅ Visit GolfNow Listing
                </a>
            """.formatted(escapeHtml(golfnowUrl));
        }
        
        section += """
                <a href="https://www.golfnow.co.uk/course-directory" target="_blank" rel="noopener noreferrer" class="btn-link" style="text-align: center; opacity: 0.7;">
                    🔍 Search GolfNow Directory
                </a>
            </div>
        </div>
        """;
        
        return section;
    }
    
    private static String renderValidationResult(ValidationResult result) {
        if (result == null) {
            return "<div class='validation-result neutral'>No validation performed</div>";
        }
        
        String cssClass = switch (result.status) {
            case OK -> "ok";
            case WARNING -> "warning";
            case ERROR -> "error";
        };
        
        String icon = switch (result.status) {
            case OK -> "✓";
            case WARNING -> "⚠";
            case ERROR -> "✗";
        };
        
        return "<div class='validation-result %s'>%s %s</div>".formatted(
            cssClass, icon, escapeHtml(result.message)
        );
    }
    
    private static String renderNavigationHeader() {
        Set<Character> availableLetters = fileNavigator.getAvailableFirstLetters();
        StringBuilder alphabetNav = new StringBuilder();
        
        // Generate A-Z letter buttons
        for (char c = 'a'; c <= 'z'; c++) {
            boolean available = availableLetters.contains(c);
            String upperLetter = String.valueOf(c).toUpperCase();
            
            if (available) {
                alphabetNav.append("""
                    <form method="post" action="/jump-to-letter" style="display: inline;">
                        <input type="hidden" name="letter" value="%s">
                        <button type="submit" class="letter-btn">%s</button>
                    </form>
                    """.formatted(c, upperLetter));
            } else {
                alphabetNav.append("""
                    <button class="letter-btn" disabled>%s</button>
                    """.formatted(upperLetter));
            }
        }
        
        return """
        <div class="nav-header">
            <div class="nav-section">
                <div class="nav-label">Jump to Letter</div>
                <div class="alphabet-nav">
                    %s
                </div>
            </div>
            <div class="nav-section">
                <div class="nav-label">Search Course</div>
                <form method="post" action="/search-course" class="search-box">
                    <input type="text" name="search" class="search-input" placeholder="Type course name..." autocomplete="off">
                    <button type="submit" class="search-btn">🔍 Search</button>
                </form>
            </div>
        </div>
        """.formatted(alphabetNav.toString());
    }
    
    private static String renderError(String title, String message) {
        return renderError(title, message, null);
    }
    
    private static String renderError(String title, String message, String preservedContent) {
        String contentSection = preservedContent != null 
            ? "<textarea style='width:100%%; height:400px; font-family:monospace;'>%s</textarea>".formatted(escapeHtml(preservedContent))
            : "";
        
        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Error - Course Audit</title>
    <style>
        body { font-family: sans-serif; padding: 40px; background: #f5f5f5; }
        .error-container { 
            max-width: 800px; 
            margin: 0 auto; 
            background: white;
            padding: 30px;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        h1 { color: #c62828; }
        .error-message { 
            background: #ffebee; 
            padding: 15px; 
            border-radius: 4px;
            border-left: 4px solid #ef5350;
            margin: 20px 0;
        }
        a { 
            color: #2196f3;
            text-decoration: none;
            font-weight: 600;
        }
        a:hover { text-decoration: underline; }
    </style>
</head>
<body>
    <div class="error-container">
        <h1>%s</h1>
        <div class="error-message">%s</div>
        %s
        <p><a href="/">← Back to audit</a></p>
    </div>
</body>
</html>
        """.formatted(title, escapeHtml(message), contentSection);
    }
    
    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
    
    private static DownloadResult downloadAndSaveImage(String imageUrl, Path yamlFile) {
        try {
            // Validate URL
            if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                return new DownloadResult(false, "URL must start with http:// or https://", null);
            }
            
            // Download image
            URL url = new URL(imageUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "CourseAudit/1.0");
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                return new DownloadResult(false, "HTTP " + responseCode + ": Could not download image", null);
            }
            
            // Read image using ImageIO to validate it's a real image
            BufferedImage image = ImageIO.read(conn.getInputStream());
            conn.disconnect();
            
            if (image == null) {
                return new DownloadResult(false, "Downloaded file is not a valid image", null);
            }
            
            // Determine file extension from URL or content type
            String extension = "jpg"; // default
            String urlLower = imageUrl.toLowerCase();
            if (urlLower.endsWith(".png")) {
                extension = "png";
            } else if (urlLower.endsWith(".webp")) {
                extension = "webp";
            } else if (urlLower.endsWith(".jpeg") || urlLower.endsWith(".jpg")) {
                extension = "jpg";
            }
            
            // Generate filename from YAML filename
            String yamlFileName = yamlFile.getFileName().toString();
            String baseName = yamlFileName.substring(0, yamlFileName.lastIndexOf("."));
            String imageFileName = baseName + "." + extension;
            
            // Save image to filesystem
            Path imagePath = Paths.get(IMAGES_PATH, imageFileName);
            ImageIO.write(image, extension, imagePath.toFile());

            Path thumbnailPath = Paths.get(THUMBS_PATH, baseName + ".jpg");
            generateThumbnail(imagePath, thumbnailPath, THUMB_WIDTH);
            
            String localPath = "/images/courses/" + imageFileName;
            return new DownloadResult(true, "Success", localPath);
            
        } catch (Exception e) {
            return new DownloadResult(false, "Error downloading image: " + e.getMessage(), null);
        }
    }

    private static void generateThumbnail(Path sourcePath, Path thumbPath, int targetWidth) throws IOException {
        BufferedImage source = ImageIO.read(sourcePath.toFile());
        if (source == null) {
            throw new IOException("Could not decode image");
        }

        int width = source.getWidth();
        int height = source.getHeight();
        if (width <= 0 || height <= 0) {
            throw new IOException("Invalid image dimensions");
        }

        int outputWidth = Math.min(targetWidth, width);
        int outputHeight = Math.max(1, (int) Math.round((double) height * outputWidth / width));

        BufferedImage resized = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(source, 0, 0, outputWidth, outputHeight, null);
        graphics.dispose();

        Files.createDirectories(thumbPath.getParent());
        writeJpeg(resized, thumbPath, THUMB_JPEG_QUALITY);
    }

    private static void writeJpeg(BufferedImage image, Path outputPath, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer available");
        }

        ImageWriter writer = writers.next();
        try (FileImageOutputStream output = new FileImageOutputStream(outputPath.toFile())) {
            writer.setOutput(output);
            ImageWriteParam params = writer.getDefaultWriteParam();
            if (params.canWriteCompressed()) {
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionQuality(quality);
            }
            writer.write(null, new IIOImage(image, null, null), params);
        } finally {
            writer.dispose();
        }
    }
    
    private static String updateImageUrlInYaml(String yamlContent, String newImageUrl) {
        // Simple line-by-line replacement of mainImageUrl
        String[] lines = yamlContent.split("\n");
        StringBuilder result = new StringBuilder();
        
        for (String line : lines) {
            if (line.trim().startsWith("mainImageUrl:")) {
                // Replace with new URL
                String indent = line.substring(0, line.indexOf("mainImageUrl:"));
                result.append(indent).append("mainImageUrl: \"").append(newImageUrl).append("\"");
            } else {
                result.append(line);
            }
            result.append("\n");
        }
        
        return result.toString();
    }

    private static String updateStayImageUrlInYaml(String yamlContent, String newStayImageUrl) {
        boolean hasStayImageField = yamlContent.contains("stayImageUrl:");
        String[] lines = yamlContent.split("\n");
        StringBuilder result = new StringBuilder();
        boolean foundStayImage = false;
        boolean addedStayImage = false;

        for (String line : lines) {
            if (line.trim().startsWith("stayImageUrl:")) {
                if (!foundStayImage) {
                    String indent = line.substring(0, line.indexOf("stayImageUrl:"));
                    if (newStayImageUrl.isEmpty()) {
                        result.append(indent).append("stayImageUrl:");
                    } else {
                        result.append(indent).append("stayImageUrl: \"").append(newStayImageUrl).append("\"");
                    }
                    foundStayImage = true;
                    result.append("\n");
                }
                continue;
            }

            result.append(line);

            if (!hasStayImageField && !addedStayImage && line.trim().startsWith("mainImageUrl:")) {
                String indent = "";
                if (line.indexOf("mainImageUrl:") > 0) {
                    indent = line.substring(0, line.indexOf("mainImageUrl:"));
                }
                if (newStayImageUrl.isEmpty()) {
                    result.append("\n").append(indent).append("stayImageUrl:");
                } else {
                    result.append("\n").append(indent).append("stayImageUrl: \"").append(newStayImageUrl).append("\"");
                }
                addedStayImage = true;
            }

            result.append("\n");
        }

        return result.toString();
    }
    
    private static String updateWebsiteUrlInYaml(String yamlContent, String newWebsiteUrl) {
        // Simple line-by-line replacement or addition of website field
        boolean hasWebsiteField = yamlContent.contains("website:");
        String[] lines = yamlContent.split("\n");
        StringBuilder result = new StringBuilder();
        boolean foundWebsite = false;
        boolean addedWebsite = false;

        for (String line : lines) {
            if (line.trim().startsWith("website:")) {
                if (!foundWebsite) {
                    // Replace the first website field found
                    String indent = line.substring(0, line.indexOf("website:"));
                    if (newWebsiteUrl.isEmpty()) {
                        result.append(indent).append("website:");
                    } else {
                        result.append(indent).append("website: \"").append(newWebsiteUrl).append("\"");
                    }
                    foundWebsite = true;
                    result.append("\n");
                }
                // Skip any duplicate website fields
                continue;
            }

            result.append(line);

            // Add website field after name if it doesn't exist anywhere
            if (!hasWebsiteField && !addedWebsite && !newWebsiteUrl.isEmpty() && line.trim().startsWith("name:")) {
                String indent = "";
                if (line.indexOf("name:") > 0) {
                    indent = line.substring(0, line.indexOf("name:"));
                }
                result.append("\n").append(indent).append("website: \"").append(newWebsiteUrl).append("\"");
                addedWebsite = true;
            }

            result.append("\n");
        }

        return result.toString();
    }
    
    private static String updateGolfNowUrlInYaml(String yamlContent, String newGolfNowUrl) {
        // Replace or add golfnowUrl field
        boolean hasGolfNowUrlField = yamlContent.contains("golfnowUrl:");
        String[] lines = yamlContent.split("\n");
        StringBuilder result = new StringBuilder();
        boolean foundGolfNowUrl = false;
        boolean addedGolfNowUrl = false;

        for (String line : lines) {
            if (line.trim().startsWith("golfnowUrl:")) {
                if (!foundGolfNowUrl) {
                    // Replace the first golfnowUrl field found
                    String indent = line.substring(0, line.indexOf("golfnowUrl:"));
                    if (newGolfNowUrl.isEmpty()) {
                        result.append(indent).append("golfnowUrl:");
                    } else {
                        result.append(indent).append("golfnowUrl: \"").append(newGolfNowUrl).append("\"");
                    }
                    foundGolfNowUrl = true;
                    result.append("\n");
                }
                // Skip any duplicate golfnowUrl fields
                continue;
            }

            result.append(line);

            // Add golfnowUrl field after website if it doesn't exist anywhere
            if (!hasGolfNowUrlField && !addedGolfNowUrl && line.trim().startsWith("website:")) {
                String indent = "";
                if (line.indexOf("website:") > 0) {
                    indent = line.substring(0, line.indexOf("website:"));
                }
                if (!newGolfNowUrl.isEmpty()) {
                    result.append("\n").append(indent).append("golfnowUrl: \"").append(newGolfNowUrl).append("\"");
                }
                addedGolfNowUrl = true;
            }

            result.append("\n");
        }

        return result.toString();
    }
    
    private static String updateClosedInYaml(String yamlContent, boolean isClosed) {
        // First check if closed field exists anywhere
        boolean hasClosedField = yamlContent.contains("closed:");
        
        // Simple line-by-line replacement or addition of closed field
        String[] lines = yamlContent.split("\n");
        StringBuilder result = new StringBuilder();
        boolean foundClosed = false;
        boolean addedClosed = false;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            
            if (line.trim().startsWith("closed:")) {
                if (!foundClosed) {
                    // Replace the first closed field found
                    String indent = line.substring(0, line.indexOf("closed:"));
                    result.append(indent).append("closed: ").append(isClosed);
                    foundClosed = true;
                    result.append("\n");
                }
                // Skip any duplicate closed fields
                continue;
            }
            
            result.append(line);
            
            // Add closed field after name if it doesn't exist anywhere
            if (!hasClosedField && !addedClosed && line.trim().startsWith("name:")) {
                String indent = "";
                if (line.indexOf("name:") > 0) {
                    indent = line.substring(0, line.indexOf("name:"));
                }
                result.append("\n").append(indent).append("closed: ").append(isClosed);
                addedClosed = true;
            }
            
            result.append("\n");
        }
        
        return result.toString();
    }

    private static String updatePlayAndStayInYaml(String yamlContent, boolean isPlayAndStay) {
        boolean hasPlayAndStayField = yamlContent.contains("playAndStay:");

        String[] lines = yamlContent.split("\n");
        StringBuilder result = new StringBuilder();
        boolean foundPlayAndStay = false;
        boolean addedPlayAndStay = false;

        for (String line : lines) {
            if (line.trim().startsWith("playAndStay:")) {
                if (!foundPlayAndStay) {
                    String indent = line.substring(0, line.indexOf("playAndStay:"));
                    result.append(indent).append("playAndStay: ").append(isPlayAndStay);
                    foundPlayAndStay = true;
                    result.append("\n");
                }
                continue;
            }

            result.append(line);

            if (!hasPlayAndStayField && !addedPlayAndStay && line.trim().startsWith("name:")) {
                String indent = "";
                if (line.indexOf("name:") > 0) {
                    indent = line.substring(0, line.indexOf("name:"));
                }
                result.append("\n").append(indent).append("playAndStay: ").append(isPlayAndStay);
                addedPlayAndStay = true;
            }

            result.append("\n");
        }

        return result.toString();
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
                    if (newAddress.isEmpty()) {
                        result.append(indent).append("address:");
                    } else {
                        result.append(indent).append("address: \"").append(newAddress).append("\"");
                    }
                    foundAddress = true;
                    result.append("\n");
                }
                continue;
            }

            result.append(line);

            if (!hasAddressField && !addedAddress && !newAddress.isEmpty() && line.trim().startsWith("name:")) {
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

    private static String updateOptionalRankInYaml(String yamlContent, String fieldName, Integer rankValue) {
        String[] lines = yamlContent.split("\n");
        StringBuilder result = new StringBuilder();
        boolean inserted = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith(fieldName + ":")) {
                continue;
            }

            result.append(line).append("\n");

            if (!inserted && trimmed.startsWith("mainImageUrl:")) {
                int indentLen = line.length() - line.stripLeading().length();
                String indent = line.substring(0, indentLen);
                if ("top100".equals(fieldName) && rankValue != null) {
                    result.append(indent).append("top100: ").append(rankValue).append("\n");
                }
                inserted = true;
            }
        }

        if (!inserted && rankValue != null) {
            result.append(fieldName).append(": ").append(rankValue).append("\n");
        }

        return result.toString();
    }

    private static String updateOptionalBooleanInYaml(String yamlContent, String fieldName, boolean enabled) {
        String[] lines = yamlContent.split("\n");
        StringBuilder result = new StringBuilder();
        boolean inserted = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith(fieldName + ":")) {
                continue;
            }

            result.append(line).append("\n");

            if (enabled && !inserted && trimmed.startsWith("mainImageUrl:")) {
                int indentLen = line.length() - line.stripLeading().length();
                String indent = line.substring(0, indentLen);
                result.append(indent).append(fieldName).append(": true").append("\n");
                inserted = true;
            }
        }

        if (enabled && !inserted) {
            result.append(fieldName).append(": true").append("\n");
        }

        return result.toString();
    }

    private static String updateLatLngInYaml(String yamlContent, double lat, double lng) {
        // Remove any existing lat/lng lines and insert new coordinates after the address line
        String[] lines = yamlContent.split("\n");
        StringBuilder result = new StringBuilder();
        boolean addedCoords = false;

        for (String line : lines) {
            if (line.trim().startsWith("lat:") || line.trim().startsWith("lng:")) {
                continue; // remove old values
            }
            result.append(line).append("\n");
            // Insert coordinates immediately after the address line
            if (!addedCoords && line.trim().startsWith("address:")) {
                // Calculate indentation from the number of leading characters before the trimmed content
                int indentLen = line.length() - line.stripLeading().length();
                String indent = line.substring(0, indentLen);
                result.append(indent).append("lat: ").append(lat).append("\n");
                result.append(indent).append("lng: ").append(lng).append("\n");
                addedCoords = true;
            }
        }

        // If no address line was present, append at the end of the file
        if (!addedCoords) {
            result.append("lat: ").append(lat).append("\n");
            result.append("lng: ").append(lng).append("\n");
        }

        return result.toString();
    }

    private static String updateNearbyInYaml(String yamlContent, String nearby1, String nearby2, String nearby3) {
        String[] lines = yamlContent.split("\n");
        StringBuilder result = new StringBuilder();
        boolean addedNearby = false;

        for (String line : lines) {
            String trimmed = line.trim();
            // Remove existing nearby lines — will re-insert in the right place
            if (trimmed.startsWith("nearby1:") || trimmed.startsWith("nearby2:") || trimmed.startsWith("nearby3:")) {
                continue;
            }
            result.append(line).append("\n");
            // Insert after the lng: line
            if (!addedNearby && trimmed.startsWith("lng:")) {
                int indentLen = line.length() - line.stripLeading().length();
                String indent = line.substring(0, indentLen);
                appendNearbyLines(result, indent, nearby1, nearby2, nearby3);
                addedNearby = true;
            }
        }

        // If there was no lng: line, append at the end
        if (!addedNearby) {
            appendNearbyLines(result, "", nearby1, nearby2, nearby3);
        }

        return result.toString();
    }

    private static void appendNearbyLines(StringBuilder sb, String indent,
                                          String nearby1, String nearby2, String nearby3) {
        if (nearby1 != null) sb.append(indent).append("nearby1: \"").append(nearby1).append("\"\n");
        if (nearby2 != null) sb.append(indent).append("nearby2: \"").append(nearby2).append("\"\n");
        if (nearby3 != null) sb.append(indent).append("nearby3: \"").append(nearby3).append("\"\n");
    }
    
    // Data classes
    record CourseData(String name, String website, String golfnowUrl, String mainImageUrl, String stayImageUrl, String region, boolean closed, boolean playAndStay, String address, Double lat, Double lng, String nearby1, String nearby2, String nearby3, Integer top100, Boolean next100) {}
    
    record ValidationResult(ValidationStatus status, String message, String dimensions) {}
    
    record DownloadResult(boolean success, String message, String localImagePath) {}
    
    enum ValidationStatus { OK, WARNING, ERROR }
    
    // File Navigator
    static class FileNavigator {
        private List<Path> courseFiles = new ArrayList<>();
        private int currentIndex = 0;
        private int closedFilteredOutCount = 0;
        
        public void loadCourseFiles(Path coursesDir, boolean showClosed) throws IOException {
            List<Path> allCourseFiles = Files.list(coursesDir)
                .filter(p -> p.toString().endsWith(".yaml"))
                .sorted()
                .collect(Collectors.toList());

            closedFilteredOutCount = 0;
            if (showClosed) {
                courseFiles = allCourseFiles;
            } else {
                courseFiles = new ArrayList<>();
                for (Path courseFile : allCourseFiles) {
                    try {
                        String yamlContent = Files.readString(courseFile);
                        CourseData course = parseCourse(yamlContent);
                        if (course.closed()) {
                            closedFilteredOutCount++;
                            continue;
                        }
                    } catch (Exception e) {
                        log.warn("[load-course-files] keeping file={} due_to=parse_error while filtering closed: {}", courseFile.getFileName(), e.getMessage());
                    }
                    courseFiles.add(courseFile);
                }
            }

            currentIndex = 0;
            
            if (courseFiles.isEmpty()) {
                if (showClosed) {
                    throw new IOException("No YAML files found in " + coursesDir);
                }
                throw new IOException("No open YAML files found in " + coursesDir + " (use --show-closed to include closed courses)");
            }
        }

        public int getClosedFilteredOutCount() {
            return closedFilteredOutCount;
        }
        
        public Path getCurrentFile() {
            if (courseFiles.isEmpty()) return null;
            return courseFiles.get(currentIndex);
        }
        
        public int getCurrentIndex() {
            return currentIndex;
        }
        
        public int getTotalFiles() {
            return courseFiles.size();
        }
        
        public boolean hasNext() {
            return currentIndex < courseFiles.size() - 1;
        }
        
        public boolean hasPrevious() {
            return currentIndex > 0;
        }
        
        public void moveNext() {
            if (hasNext()) {
                currentIndex++;
            }
        }
        
        public void movePrevious() {
            if (hasPrevious()) {
                currentIndex--;
            }
        }
        
        public void jumpToIndex(int index) {
            if (index >= 0 && index < courseFiles.size()) {
                currentIndex = index;
            }
        }
        
        public int findFirstCourseStartingWith(char letter) {
            String targetLetter = String.valueOf(letter).toLowerCase();
            for (int i = 0; i < courseFiles.size(); i++) {
                String fileName = courseFiles.get(i).getFileName().toString().toLowerCase();
                if (fileName.startsWith(targetLetter)) {
                    return i;
                }
            }
            return -1; // Not found
        }
        
        public int findCourseByName(String searchTerm) {
            String searchLower = searchTerm.toLowerCase().trim();
            for (int i = 0; i < courseFiles.size(); i++) {
                String fileName = courseFiles.get(i).getFileName().toString().toLowerCase();
                if (fileName.contains(searchLower)) {
                    return i;
                }
            }
            return -1; // Not found
        }
        
        public List<Map<String, Object>> getAllCoursesInfo() {
            List<Map<String, Object>> courses = new ArrayList<>();
            for (int i = 0; i < courseFiles.size(); i++) {
                Map<String, Object> info = new HashMap<>();
                String fileName = courseFiles.get(i).getFileName().toString();
                String displayName = fileName.replace(".yaml", "").replace("-", " ");
                info.put("index", i);
                info.put("fileName", fileName);
                info.put("displayName", displayName);
                courses.add(info);
            }
            return courses;
        }
        
        public Set<Character> getAvailableFirstLetters() {
            Set<Character> letters = new TreeSet<>();
            for (Path file : courseFiles) {
                String fileName = file.getFileName().toString();
                if (!fileName.isEmpty()) {
                    letters.add(Character.toLowerCase(fileName.charAt(0)));
                }
            }
            return letters;
        }
    }
    
    // Validator
    static class Validator {
        private static final int TIMEOUT_MS = 5000;
        
        public ValidationResult validateWebsite(String url) {
            if (url == null || url.isEmpty()) {
                return new ValidationResult(ValidationStatus.OK, "No website URL provided (optional)", null);
            }
            
            // Format check
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return new ValidationResult(ValidationStatus.WARNING, 
                    "URL should start with http:// or https://: " + url, null);
            }
            
            // HTTP check
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent", "CourseAudit/1.0");
                
                int responseCode = conn.getResponseCode();
                conn.disconnect();
                
                if (responseCode >= 200 && responseCode < 400) {
                    return new ValidationResult(ValidationStatus.OK, 
                        "Website accessible (HTTP " + responseCode + ")", null);
                } else {
                    return new ValidationResult(ValidationStatus.ERROR, 
                        "Website returned HTTP " + responseCode, null);
                }
            } catch (java.net.SocketTimeoutException e) {
                return new ValidationResult(ValidationStatus.ERROR, 
                    "Website timeout (>5s): " + url, null);
            } catch (Exception e) {
                return new ValidationResult(ValidationStatus.ERROR, 
                    "Cannot access website: " + e.getMessage(), null);
            }
        }
        
        public ValidationResult validateImage(String imageUrl) {
            if (imageUrl == null || imageUrl.isEmpty()) {
                return new ValidationResult(ValidationStatus.WARNING, 
                    "No image URL provided", null);
            }
            
            // Check if placeholder
            if (PLACEHOLDER_IMAGE.equals(imageUrl)) {
                String dimensions = getImageDimensions(imageUrl);
                return new ValidationResult(ValidationStatus.WARNING, 
                    "Using placeholder image", dimensions);
            }
            
            // Local file check
            if (imageUrl.startsWith("/images/courses/")) {
                String filename = imageUrl.substring("/images/courses/".length());
                Path imageFile = Paths.get(IMAGES_PATH, filename);
                
                if (!Files.exists(imageFile)) {
                    return new ValidationResult(ValidationStatus.ERROR, 
                        "Image file not found: " + imageFile.toAbsolutePath(), null);
                }
                
                String dimensions = getImageDimensions(imageUrl);
                return new ValidationResult(ValidationStatus.OK, 
                    "Image file exists", dimensions);
            }
            
            // External URL - try to validate
            if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(imageUrl).openConnection();
                    conn.setRequestMethod("HEAD");
                    conn.setConnectTimeout(TIMEOUT_MS);
                    conn.setReadTimeout(TIMEOUT_MS);
                    conn.setRequestProperty("User-Agent", "CourseAudit/1.0");
                    
                    int responseCode = conn.getResponseCode();
                    conn.disconnect();
                    
                    if (responseCode >= 200 && responseCode < 400) {
                        return new ValidationResult(ValidationStatus.OK, 
                            "External image accessible (HTTP " + responseCode + ")", null);
                    } else {
                        return new ValidationResult(ValidationStatus.ERROR, 
                            "External image returned HTTP " + responseCode, null);
                    }
                } catch (Exception e) {
                    return new ValidationResult(ValidationStatus.ERROR, 
                        "Cannot access external image: " + e.getMessage(), null);
                }
            }
            
            return new ValidationResult(ValidationStatus.WARNING, 
                "Unrecognized image URL format: " + imageUrl, null);
        }
        
        private String getImageDimensions(String imageUrl) {
            try {
                BufferedImage image;
                
                if (imageUrl.startsWith("/images/courses/")) {
                    String filename = imageUrl.substring("/images/courses/".length());
                    Path imageFile = Paths.get(IMAGES_PATH, filename);
                    
                    if (!Files.exists(imageFile)) {
                        return null;
                    }
                    
                    image = ImageIO.read(imageFile.toFile());
                } else if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                    URL url = new URL(imageUrl);
                    image = ImageIO.read(url);
                } else {
                    return null;
                }
                
                if (image == null) {
                    return "Dimensions unavailable";
                }
                
                return image.getWidth() + " × " + image.getHeight();
            } catch (Exception e) {
                return "Dimensions unavailable";
            }
        }
    }
}
