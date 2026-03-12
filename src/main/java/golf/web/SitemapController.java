package golf.web;

import golf.course.Courses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Controller
@RequiredArgsConstructor
public class SitemapController {

    private final Courses courses;

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public void sitemap(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.APPLICATION_XML_VALUE);
        response.setCharacterEncoding("UTF-8");

        String baseUrl = buildBaseUrl(request);
        PrintWriter writer = response.getWriter();
        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        writer.println("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");

        writeUrl(writer, baseUrl + "/", "weekly", "1.0");
        writeUrl(writer, baseUrl + "/courses", "weekly", "0.9");
        writeUrl(writer, baseUrl + "/map", "monthly", "0.7");
        writeUrl(writer, baseUrl + "/top-100", "monthly", "0.8");
        writeUrl(writer, baseUrl + "/next-100", "monthly", "0.8");
        writeUrl(writer, baseUrl + "/play-and-stay", "monthly", "0.8");
        writeUrl(writer, baseUrl + "/challenge", "monthly", "0.7");

        courses.getAllCourses().forEach(course -> {
            String slug = Courses.toCourseSlug(course.name());
            writeUrl(writer, baseUrl + "/courses/" + slug, "monthly", "0.8");
        });

        writer.println("</urlset>");
        writer.flush();
    }

    private static String buildBaseUrl(HttpServletRequest request) {
        String scheme = request.getHeader("X-Forwarded-Proto");
        if (scheme == null || scheme.isBlank()) {
            scheme = request.getScheme();
        }
        String host = request.getHeader("X-Forwarded-Host");
        if (host == null || host.isBlank()) {
            host = request.getServerName();
            int port = request.getServerPort();
            if (("http".equals(scheme) && port != 80) || ("https".equals(scheme) && port != 443)) {
                host = host + ":" + port;
            }
        }
        return scheme + "://" + host;
    }

    private static void writeUrl(PrintWriter writer, String loc, String changefreq, String priority) {
        writer.println("  <url>");
        writer.println("    <loc>" + xmlEscape(loc) + "</loc>");
        writer.println("    <changefreq>" + changefreq + "</changefreq>");
        writer.println("    <priority>" + priority + "</priority>");
        writer.println("  </url>");
    }

    private static String xmlEscape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

}
