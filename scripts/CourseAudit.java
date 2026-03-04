///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javalin:javalin:5.6.3
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.16.1
//DEPS com.fasterxml.jackson.core:jackson-databind:2.16.1
//DEPS org.slf4j:slf4j-simple:2.0.9
//DEPS com.twelvemonkeys.imageio:imageio-webp:3.10.1
//JAVA 21

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.javalin.Javalin;
import io.javalin.http.Context;

import javax.imageio.ImageIO;
import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class CourseAudit {

    private static final int PORT = 7070;
    private static final String COURSES_PATH = "src/main/resources/courses";
    private static final String IMAGES_PATH = "src/main/resources/static/images/courses";
    private static final String PLACEHOLDER_IMAGE = "/images/courses/placeholder-course.jpg";
    
    private static final FileNavigator fileNavigator = new FileNavigator();
    private static final Validator validator = new Validator();

    public static void main(String[] args) throws IOException {
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
        fileNavigator.loadCourseFiles(coursesDir);
        System.out.println("Loaded " + fileNavigator.getTotalFiles() + " course YAML files");
        
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
        app.post("/update-closed", CourseAudit::handleUpdateClosed);
        app.post("/download-image", CourseAudit::handleDownloadImage);
        
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
            
            ctx.html(renderHTML(fileName, yamlContent, course, websiteValidation, imageValidation, null));
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
    
    private static CourseData parseCourse(String yaml) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Map<String, Object> data = mapper.readValue(yaml, Map.class);
        
        String name = (String) data.get("name");
        String website = (String) data.get("website");
        String mainImageUrl = (String) data.get("mainImageUrl");
        
        // Parse closed field - handle both Boolean and String types
        boolean closed = false;
        Object closedObj = data.get("closed");
        if (closedObj instanceof Boolean) {
            closed = (Boolean) closedObj;
        } else if (closedObj instanceof String) {
            closed = "true".equalsIgnoreCase((String) closedObj);
        }
        
        // Extract region.name
        String region = null;
        Object regionObj = data.get("region");
        if (regionObj instanceof Map) {
            region = (String) ((Map<?, ?>) regionObj).get("name");
        }
        
        return new CourseData(name, website, mainImageUrl, region, closed);
    }
    
    private static String renderHTML(String fileName, String yamlContent, CourseData course,
                                     ValidationResult websiteValidation, ValidationResult imageValidation,
                                     String message) {
        int current = fileNavigator.getCurrentIndex() + 1;
        int total = fileNavigator.getTotalFiles();
        boolean hasNext = fileNavigator.hasNext();
        boolean hasPrev = fileNavigator.hasPrevious();
        
        // Image preview
        String imagePreview = renderImagePreview(course.mainImageUrl, imageValidation);
        
        // Website validation with visit button
        String websiteSection = renderWebsiteValidation(course.website, websiteValidation);
        
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
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>Course Audit: %s%s</h1>
            <div class="progress">File %d of %d</div>
        </div>
        
        %s
        
        <form method="post">
            <div class="content-area">
                <div class="left-panel">
                    <div class="image-section">
                        <div class="validation-label">Image Preview</div>
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
                                           onchange="this.form.action='/update-closed'; this.form.submit();" 
                                           style="width: 20px; height: 20px; margin-right: 10px; cursor: pointer;">
                                    <span style="font-size: 14px;"><strong>Mark as CLOSED</strong></span>
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
                            <div style="margin-bottom: 10px;"><strong>Status:</strong> <span style="color: %s; font-weight: bold;">%s</span></div>
                            <div><strong>File:</strong> <code style="background: #e0e0e0; padding: 2px 6px; border-radius: 3px; font-size: 12px;">%s</code></div>
                        </div>
                    </div>
                </div>
            </div>
            
            <div class="button-row">
                <div class="button-group">
                    <button type="submit" formaction="/previous" class="btn-secondary" %s>← Previous</button>
                </div>
                <div class="button-group">
                    <button type="submit" formaction="/next" class="btn-success" %s>Next →</button>
                </div>
            </div>
        </form>
    </div>
</body>
</html>
        """.formatted(
            fileName,
            fileName,  // Add fileName again for h1
            course.closed ? "<span class='closed-badge'>⚠️ CLOSED</span>" : "",
            current, 
            total,
            message != null ? message : "",
            imagePreview,
            "%WEBSITE_SECTION%",
            renderValidationResult(imageValidation),
            escapeHtml(course.website != null ? course.website : ""),
            course.closed ? "checked" : "",
            escapeHtml(course.name != null ? course.name : "N/A"),
            escapeHtml(course.region != null ? course.region : "N/A"),
            course.closed ? "#ef5350" : "#4caf50",
            course.closed ? "CLOSED" : "OPEN",
            fileName,
            hasPrev ? "" : "disabled",
            hasNext ? "" : "disabled"
        ).replace("%WEBSITE_SECTION%", websiteSection);
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
            
            String localPath = "/images/courses/" + imageFileName;
            return new DownloadResult(true, "Success", localPath);
            
        } catch (Exception e) {
            return new DownloadResult(false, "Error downloading image: " + e.getMessage(), null);
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
    
    private static String updateWebsiteUrlInYaml(String yamlContent, String newWebsiteUrl) {
        // Simple line-by-line replacement of website
        String[] lines = yamlContent.split("\n");
        StringBuilder result = new StringBuilder();
        boolean foundWebsite = false;
        
        for (String line : lines) {
            if (line.trim().startsWith("website:")) {
                // Replace with new URL
                String indent = line.substring(0, line.indexOf("website:"));
                if (newWebsiteUrl.isEmpty()) {
                    result.append(indent).append("website:");
                } else {
                    result.append(indent).append("website: \"").append(newWebsiteUrl).append("\"");
                }
                foundWebsite = true;
            } else {
                result.append(line);
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
    
    // Data classes
    record CourseData(String name, String website, String mainImageUrl, String region, boolean closed) {}
    
    record ValidationResult(ValidationStatus status, String message, String dimensions) {}
    
    record DownloadResult(boolean success, String message, String localImagePath) {}
    
    enum ValidationStatus { OK, WARNING, ERROR }
    
    // File Navigator
    static class FileNavigator {
        private List<Path> courseFiles = new ArrayList<>();
        private int currentIndex = 0;
        
        public void loadCourseFiles(Path coursesDir) throws IOException {
            courseFiles = Files.list(coursesDir)
                .filter(p -> p.toString().endsWith(".yaml"))
                .sorted()
                .collect(Collectors.toList());
            
            if (courseFiles.isEmpty()) {
                throw new IOException("No YAML files found in " + coursesDir);
            }
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
