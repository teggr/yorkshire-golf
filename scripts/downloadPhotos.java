///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21
//DEPS io.javalin:javalin:6.7.0
//DEPS me.friwi:jcefmaven:143.0.14
//DEPS org.slf4j:slf4j-simple:2.0.9
//DEPS com.fasterxml.jackson.core:jackson-databind:2.18.3

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.impl.progress.ConsoleProgressHandler;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;

/**
 * Google Photos Downloader - jbang script
 *
 * Usage:
 *   1. Place credentials.json (Google OAuth Desktop credentials) in the current directory
 *   2. Run: jbang scripts/downloadPhotos.java
 *
 * First run will download JCEF binaries into jcef-bundle/ (one-time, ~150 MB).
 */
public class downloadPhotos {

    static final int PORT = 8090;
    static final String REDIRECT_URI = "http://localhost:" + PORT + "/callback";
    static final String PHOTOS_SCOPE = "https://www.googleapis.com/auth/photoslibrary.readonly";
    static final String AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    static final String PHOTOS_API_BASE = "https://photoslibrary.googleapis.com/v1/mediaItems";

    static volatile String accessToken = null;
    static String clientId;
    static String clientSecret;
    static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        loadCredentials();

        Javalin app = Javalin.create().start(PORT);
        setupRoutes(app);

        System.out.println("Starting browser (may download JCEF on first run ~150 MB)...");

        CefAppBuilder builder = new CefAppBuilder();
        builder.setInstallDir(new File("jcef-bundle"));
        builder.setProgressHandler(new ConsoleProgressHandler());
        builder.addJcefArgs("--disable-gpu",
            "--disable-dev-shm-usage",
            // --no-sandbox is required when running as root or in restricted container environments.
            // Remove the next line for normal desktop use to keep Chromium's sandbox active.
            "--no-sandbox");

        CefApp cefApp = builder.build();
        CefClient cefClient = cefApp.createClient();
        CefBrowser cefBrowser = cefClient.createBrowser("http://localhost:" + PORT, false, false);

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Google Photos Downloader");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(cefBrowser.getUIComponent(), BorderLayout.CENTER);
            frame.setSize(1280, 900);
            frame.setVisible(true);
        });
    }

    static void loadCredentials() throws Exception {
        File file = new File("credentials.json");
        if (!file.exists()) {
            throw new IllegalStateException(
                "credentials.json not found.\n" +
                "Download it from Google Cloud Console:\n" +
                "  1. Go to https://console.cloud.google.com/\n" +
                "  2. Enable the Google Photos Library API\n" +
                "  3. Create OAuth 2.0 credentials (Desktop type)\n" +
                "  4. Download and save as credentials.json in the current directory"
            );
        }
        JsonNode root = MAPPER.readTree(file);
        JsonNode creds = root.has("installed") ? root.get("installed") : root.get("web");
        clientId = creds.get("client_id").asText();
        clientSecret = creds.get("client_secret").asText();
    }

    static void setupRoutes(Javalin app) {
        app.get("/", ctx -> {
            if (accessToken != null) {
                ctx.redirect("/photos");
            } else {
                ctx.html(loginPage());
            }
        });

        app.get("/auth", ctx -> ctx.redirect(buildAuthUrl()));

        app.get("/callback", ctx -> {
            String error = ctx.queryParam("error");
            if (error != null) {
                ctx.status(400).html("<h1>Authorization denied: " + escHtml(error) + "</h1>");
                return;
            }
            String code = ctx.queryParam("code");
            if (code == null) {
                ctx.status(400).html("<h1>Missing authorization code</h1>");
                return;
            }
            try {
                accessToken = exchangeCode(code);
                ctx.redirect("/photos");
            } catch (Exception e) {
                ctx.status(500).html("<h1>Authentication failed: " + escHtml(e.getMessage()) + "</h1>");
            }
        });

        app.get("/photos", ctx -> {
            if (accessToken == null) {
                ctx.redirect("/");
                return;
            }
            try {
                Map<String, Object> data = fetchPhotos(null);
                ctx.html(photoPickerPage(data));
            } catch (Exception e) {
                ctx.status(500).html("<h1>Error loading photos: " + escHtml(e.getMessage()) + "</h1>");
            }
        });

        app.get("/photos/more", ctx -> {
            if (accessToken == null) {
                ctx.status(401).result("Unauthorized");
                return;
            }
            String pageToken = ctx.queryParam("pageToken");
            try {
                Map<String, Object> data = fetchPhotos(pageToken);
                ctx.contentType("application/json").result(MAPPER.writeValueAsString(data));
            } catch (Exception e) {
                ctx.status(500).result(e.getMessage());
            }
        });

        app.post("/download", ctx -> {
            if (accessToken == null) {
                ctx.status(401).result("Unauthorized");
                return;
            }
            JsonNode body = MAPPER.readTree(ctx.body());
            String dirName = body.get("directory").asText("photos-" + LocalDate.now());
            // Allow only alphanumeric, dash, dot, and underscore — no path separators
            dirName = dirName.replaceAll("[^a-zA-Z0-9._\\-]", "_");
            if (dirName.isBlank() || dirName.contains("..")) {
                ctx.status(400).result("Invalid directory name");
                return;
            }
            JsonNode items = body.get("items");
            var outDir = Paths.get(dirName).normalize();
            Files.createDirectories(outDir);
            HttpClient http = HttpClient.newHttpClient();
            int success = 0;
            int failed = 0;
            for (JsonNode item : items) {
                String baseUrl = item.get("baseUrl").asText();
                // Strip any path components from the filename before sanitizing
                String filename = sanitizeFilename(
                    Paths.get(item.get("filename").asText()).getFileName().toString());
                try {
                    HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "=d"))
                        .GET()
                        .build();
                    HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
                    Files.write(outDir.resolve(filename), resp.body());
                    success++;
                } catch (Exception e) {
                    System.err.println("Failed to download " + filename + ": " + e.getMessage());
                    failed++;
                }
            }
            String msg = "Downloaded " + success + " photo(s) to: " + outDir.toAbsolutePath();
            if (failed > 0) msg += " (" + failed + " failed - check console)";
            ctx.result(msg);
        });
    }

    static String buildAuthUrl() {
        return AUTH_ENDPOINT + "?" +
            "client_id=" + encode(clientId) +
            "&redirect_uri=" + encode(REDIRECT_URI) +
            "&response_type=code" +
            "&scope=" + encode(PHOTOS_SCOPE) +
            "&access_type=offline" +
            "&prompt=consent";
    }

    static String exchangeCode(String code) throws Exception {
        String formBody = "code=" + encode(code) +
            "&client_id=" + encode(clientId) +
            "&client_secret=" + encode(clientSecret) +
            "&redirect_uri=" + encode(REDIRECT_URI) +
            "&grant_type=authorization_code";
        HttpClient http = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(TOKEN_ENDPOINT))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(formBody))
            .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode json = MAPPER.readTree(resp.body());
        if (!json.has("access_token")) {
            throw new RuntimeException("Token exchange failed: " + resp.body());
        }
        return json.get("access_token").asText();
    }

    static Map<String, Object> fetchPhotos(String pageToken) throws Exception {
        String url = PHOTOS_API_BASE + "?pageSize=50";
        if (pageToken != null && !pageToken.isBlank()) {
            url += "&pageToken=" + encode(pageToken);
        }
        HttpClient http = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + accessToken)
            .GET()
            .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode json = MAPPER.readTree(resp.body());

        List<Map<String, String>> items = new ArrayList<>();
        if (json.has("mediaItems")) {
            for (JsonNode item : json.get("mediaItems")) {
                if (!item.has("mediaMetadata") || !item.get("mediaMetadata").has("photo")) continue;
                String id = item.get("id").asText();
                String baseUrl = item.get("baseUrl").asText();
                String filename = item.has("filename") ? item.get("filename").asText() : id + ".jpg";
                items.add(Map.of("id", id, "baseUrl", baseUrl, "filename", filename));
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("nextPageToken", json.has("nextPageToken") ? json.get("nextPageToken").asText() : null);
        return result;
    }

    @SuppressWarnings("unchecked")
    static String photoPickerPage(Map<String, Object> data) throws Exception {
        List<Map<String, String>> items = (List<Map<String, String>>) data.get("items");
        String nextPageToken = (String) data.get("nextPageToken");
        String escapedNextToken = nextPageToken != null ? escHtml(nextPageToken) : "";
        boolean hasMore = nextPageToken != null;
        String defaultDir = "photos-" + LocalDate.now();

        StringBuilder thumbsHtml = new StringBuilder();
        for (Map<String, String> item : items) {
            String id = escHtml(item.get("id"));
            String baseUrl = escHtml(item.get("baseUrl"));
            String filename = escHtml(item.get("filename"));
            thumbsHtml.append(String.format(
                "<div class=\"photo-item\" data-id=\"%s\" data-url=\"%s\" data-filename=\"%s\">" +
                "<div class=\"photo-wrapper\">" +
                "<img src=\"%s=w200-h200-c\" alt=\"%s\" loading=\"lazy\"/>" +
                "<div class=\"check-overlay\"><span class=\"checkmark\">&#10003;</span></div>" +
                "</div>" +
                "<span class=\"photo-name\" title=\"%s\">%s</span>" +
                "</div>",
                id, baseUrl, filename, baseUrl, filename, filename, filename));
        }

        String loadMoreAttr = hasMore ? "" : "style=\"display:none\"";
        String nextTokenJs = hasMore ? "\"" + escapedNextToken + "\"" : "null";
        String emptyMsg = items.isEmpty() ? "<div class=\"empty\">No photos found in your library.</div>" : "";

        return "<!DOCTYPE html>" +
            "<html lang=\"en\">" +
            "<head>" +
            "<meta charset=\"UTF-8\">" +
            "<title>Google Photos - Select Photos</title>" +
            "<style>" +
            "* { box-sizing: border-box; margin: 0; padding: 0; }" +
            "body { font-family: Arial, sans-serif; background: #f0f2f5; }" +
            ".header { background: white; padding: 14px 20px; display: flex; align-items: center;" +
            "  justify-content: space-between; box-shadow: 0 2px 6px rgba(0,0,0,.1);" +
            "  position: sticky; top: 0; z-index: 100; flex-wrap: wrap; gap: 10px; }" +
            ".header h1 { font-size: 18px; color: #333; white-space: nowrap; }" +
            ".controls { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }" +
            ".btn { padding: 8px 14px; border: none; border-radius: 6px; cursor: pointer;" +
            "  font-size: 13px; font-weight: 500; white-space: nowrap; }" +
            ".btn-primary { background: #4285f4; color: white; }" +
            ".btn-primary:hover { background: #3367d6; }" +
            ".btn-secondary { background: #e8eaed; color: #333; }" +
            ".btn-secondary:hover { background: #dadce0; }" +
            ".dir-input { padding: 7px 10px; border: 1px solid #ddd; border-radius: 6px;" +
            "  font-size: 13px; width: 200px; }" +
            ".count-badge { background: #e8f0fe; color: #1a73e8; padding: 4px 10px;" +
            "  border-radius: 12px; font-size: 13px; font-weight: 600; }" +
            ".grid { display: grid;" +
            "  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));" +
            "  gap: 10px; padding: 20px; }" +
            ".photo-item { background: white; border-radius: 8px; overflow: hidden;" +
            "  cursor: pointer; transition: transform .15s, box-shadow .15s;" +
            "  user-select: none; position: relative; }" +
            ".photo-item:hover { transform: scale(1.03); box-shadow: 0 4px 14px rgba(0,0,0,.15); }" +
            ".photo-item.selected { outline: 3px solid #4285f4; outline-offset: -3px; }" +
            ".photo-wrapper { width: 100%; height: 180px; overflow: hidden; position: relative; }" +
            ".photo-wrapper img { width: 100%; height: 100%; object-fit: cover; display: block;" +
            "  pointer-events: none; }" +
            ".check-overlay { position: absolute; top: 6px; right: 6px; width: 26px; height: 26px;" +
            "  border-radius: 50%; background: rgba(255,255,255,0.85);" +
            "  display: flex; align-items: center; justify-content: center;" +
            "  opacity: 0; transition: opacity .15s; }" +
            ".photo-item:hover .check-overlay, .photo-item.selected .check-overlay { opacity: 1; }" +
            ".photo-item.selected .check-overlay { background: #4285f4; }" +
            ".checkmark { color: #4285f4; font-size: 16px; font-weight: bold; }" +
            ".photo-item.selected .checkmark { color: white; }" +
            ".photo-name { display: block; padding: 6px 8px; font-size: 11px; color: #555;" +
            "  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }" +
            ".load-more-wrap { text-align: center; padding: 20px; }" +
            ".load-more { padding: 10px 28px; background: #4285f4; color: white; border: none;" +
            "  border-radius: 6px; cursor: pointer; font-size: 14px; }" +
            ".load-more:disabled { background: #9aa0a6; cursor: default; }" +
            ".empty { text-align: center; padding: 60px 20px; color: #888; font-size: 18px; }" +
            "#toast { position: fixed; bottom: 20px; left: 50%;" +
            "  transform: translateX(-50%); background: #333; color: white;" +
            "  padding: 12px 24px; border-radius: 8px; font-size: 14px;" +
            "  display: none; z-index: 999; max-width: 80%; text-align: center;" +
            "  box-shadow: 0 4px 12px rgba(0,0,0,.3); }" +
            "</style></head>" +
            "<body>" +
            "<div class=\"header\">" +
            "<h1>&#128248; Google Photos</h1>" +
            "<div class=\"controls\">" +
            "<span class=\"count-badge\" id=\"count\">0 selected</span>" +
            "<button class=\"btn btn-secondary\" onclick=\"selectAll()\">Select All</button>" +
            "<button class=\"btn btn-secondary\" onclick=\"clearSelection()\">Clear</button>" +
            "<input type=\"text\" class=\"dir-input\" id=\"dirName\" placeholder=\"Folder name\" value=\"" + defaultDir + "\"/>" +
            "<button class=\"btn btn-primary\" onclick=\"downloadSelected()\">&#11015; Download</button>" +
            "</div></div>" +
            "<div class=\"grid\" id=\"grid\">" + thumbsHtml + emptyMsg + "</div>" +
            "<div class=\"load-more-wrap\">" +
            "<button class=\"load-more\" id=\"loadMoreBtn\" " + loadMoreAttr + " onclick=\"loadMore()\">Load More Photos</button>" +
            "</div>" +
            "<div id=\"toast\"></div>" +
            "<script>" +
            "var selectedItems = new Map();" +
            "var nextPageToken = " + nextTokenJs + ";" +
            "function attachHandlers(item) {" +
            "  item.addEventListener('click', function() { toggleSelect(this); });" +
            "}" +
            "document.querySelectorAll('.photo-item').forEach(attachHandlers);" +
            "function toggleSelect(el) {" +
            "  var id = el.dataset.id;" +
            "  if (selectedItems.has(id)) {" +
            "    selectedItems.delete(id); el.classList.remove('selected');" +
            "  } else {" +
            "    selectedItems.set(id, {id: el.dataset.id, baseUrl: el.dataset.url, filename: el.dataset.filename});" +
            "    el.classList.add('selected');" +
            "  }" +
            "  updateCount();" +
            "}" +
            "function updateCount() {" +
            "  document.getElementById('count').textContent = selectedItems.size + ' selected';" +
            "}" +
            "function selectAll() {" +
            "  document.querySelectorAll('.photo-item').forEach(function(el) {" +
            "    if (!selectedItems.has(el.dataset.id)) {" +
            "      selectedItems.set(el.dataset.id, {id: el.dataset.id, baseUrl: el.dataset.url, filename: el.dataset.filename});" +
            "      el.classList.add('selected');" +
            "    }" +
            "  });" +
            "  updateCount();" +
            "}" +
            "function clearSelection() {" +
            "  selectedItems.clear();" +
            "  document.querySelectorAll('.photo-item').forEach(function(el) { el.classList.remove('selected'); });" +
            "  updateCount();" +
            "}" +
            "async function downloadSelected() {" +
            "  if (selectedItems.size === 0) { showToast('Please select at least one photo'); return; }" +
            "  var dirName = document.getElementById('dirName').value.trim() || ('photos-' + new Date().toISOString().slice(0,10));" +
            "  var items = Array.from(selectedItems.values());" +
            "  showToast('Downloading ' + items.length + ' photo(s)...', 0);" +
            "  try {" +
            "    var resp = await fetch('/download', {" +
            "      method: 'POST'," +
            "      headers: {'Content-Type': 'application/json'}," +
            "      body: JSON.stringify({directory: dirName, items: items})" +
            "    });" +
            "    showToast(await resp.text(), 8000);" +
            "  } catch(e) { showToast('Error: ' + e.message, 6000); }" +
            "}" +
            "async function loadMore() {" +
            "  if (!nextPageToken) return;" +
            "  var btn = document.getElementById('loadMoreBtn');" +
            "  btn.disabled = true; btn.textContent = 'Loading...';" +
            "  try {" +
            "    var resp = await fetch('/photos/more?pageToken=' + encodeURIComponent(nextPageToken));" +
            "    var data = await resp.json();" +
            "    nextPageToken = data.nextPageToken;" +
            "    appendItems(data.items);" +
            "    if (!nextPageToken) btn.style.display = 'none';" +
            "    else { btn.disabled = false; btn.textContent = 'Load More Photos'; }" +
            "  } catch(e) {" +
            "    btn.disabled = false; btn.textContent = 'Load More Photos';" +
            "    showToast('Error loading more: ' + e.message, 4000);" +
            "  }" +
            "}" +
            "function appendItems(items) {" +
            "  var grid = document.getElementById('grid');" +
            "  items.forEach(function(item) {" +
            "    var div = document.createElement('div');" +
            "    div.className = 'photo-item';" +
            "    div.dataset.id = item.id;" +
            "    div.dataset.url = item.baseUrl;" +
            "    div.dataset.filename = item.filename;" +
            "    div.innerHTML = '<div class=\"photo-wrapper\">' +" +
            "      '<img src=\"' + esc(item.baseUrl) + '=w200-h200-c\" alt=\"' + esc(item.filename) + '\" loading=\"lazy\"/>' +" +
            "      '<div class=\"check-overlay\"><span class=\"checkmark\">&#10003;</span></div>' +" +
            "      '</div>' +" +
            "      '<span class=\"photo-name\" title=\"' + esc(item.filename) + '\">' + esc(item.filename) + '</span>';" +
            "    attachHandlers(div);" +
            "    grid.appendChild(div);" +
            "  });" +
            "}" +
            "function esc(s) {" +
            "  return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/\"/g,'&quot;');" +
            "}" +
            "function showToast(msg, dur) {" +
            "  var t = document.getElementById('toast');" +
            "  t.textContent = msg; t.style.display = 'block';" +
            "  if (dur > 0) setTimeout(function() { t.style.display = 'none'; }, dur);" +
            "}" +
            "</script></body></html>";
    }

    static String loginPage() {
        return "<!DOCTYPE html>" +
            "<html lang=\"en\">" +
            "<head>" +
            "<meta charset=\"UTF-8\">" +
            "<title>Google Photos Downloader</title>" +
            "<style>" +
            "body { font-family: Arial, sans-serif; display: flex; justify-content: center;" +
            "  align-items: center; height: 100vh; margin: 0; background: #f0f2f5; }" +
            ".card { background: white; padding: 40px; border-radius: 12px;" +
            "  box-shadow: 0 4px 20px rgba(0,0,0,0.1); text-align: center; max-width: 400px; }" +
            "h1 { color: #333; margin-bottom: 8px; font-size: 24px; }" +
            "p { color: #666; margin-bottom: 28px; }" +
            ".btn { display: inline-block; padding: 12px 28px; background: #4285f4;" +
            "  color: white; text-decoration: none; border-radius: 6px;" +
            "  font-size: 16px; font-weight: 500; }" +
            ".btn:hover { background: #3367d6; }" +
            "</style></head>" +
            "<body><div class=\"card\">" +
            "<h1>&#128248; Google Photos Downloader</h1>" +
            "<p>Sign in with Google to browse and download your photos.</p>" +
            "<a href=\"/auth\" class=\"btn\">Sign in with Google</a>" +
            "</div></body></html>";
    }

    static String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }

    static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    static String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
