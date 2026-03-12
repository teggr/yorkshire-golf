package golf.course;

import com.fasterxml.jackson.databind.ObjectMapper;
import golf.web.YorkshireGolfPageTemplate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.View;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.iframe;
import static j2html.TagCreator.p;
import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.script;

@Component
public class MapPage implements View {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public @Nullable String getContentType() {
        return MediaType.TEXT_HTML_VALUE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.setCharacterEncoding("UTF-8");

        List<MapPoint> mapPoints = model != null && model.get("mapPoints") != null
                ? (List<MapPoint>) model.get("mapPoints")
                : List.of();
        String googleMapsApiKey = model != null && model.get("googleMapsApiKey") instanceof String key
                ? key
                : "";

        new YorkshireGolfPageTemplate()
                .withRequest(request)
                .withCurrentPageBasePath("/map")
                .withTitle("Course Map – Yorkshire Golf")
                .withDescription("View all Yorkshire golf courses on an interactive map. Explore clubs across North, East, South and West Yorkshire and find courses near you.")
                .withPageScripts(
                        script(rawHtml(mapConfigScriptBody(mapPoints))),
                        script().withSrc("/js/map-page.js").attr("defer", ""),
                        googleMapsApiKey.isBlank()
                                ? script(rawHtml(""))
                                : script().withSrc(googleMapsScriptUrl(googleMapsApiKey)).attr("async", "").attr("defer", "")
                )
                .withBody(
                        div().withClass("ygl-page-header").with(
                                div().withClass("container").with(
                                        h1("Yorkshire Course Map").withClass("ygl-page-header__title"),
                                        p("Pan and zoom around Yorkshire to explore every open course location.").withClass("ygl-page-header__lead")
                                )
                        ),
                        div().withClass("container ygl-page").with(
                                div().withClass("ygl-map-page").with(
                                        p("Use the map below to browse all open Yorkshire golf courses.").withClass("ygl-page__lead"),
                                        googleMapsApiKey.isBlank()
                                                ? div().withClass("ratio ratio-16x9 ygl-map-page__map").with(
                                                        iframe()
                                                                .withSrc(fallbackMapUrl())
                                                                .withTitle("Yorkshire course map")
                                                                .attr("style", "border:0;")
                                                                .attr("loading", "lazy")
                                                                .attr("referrerpolicy", "no-referrer-when-downgrade")
                                                                .attr("allowfullscreen", "")
                                                )
                                                : div().withId("course-map").withClass("ygl-map-page__map"),
                                        googleMapsApiKey.isBlank()
                                                ? p("Google Maps API key is not configured, so a basic map is shown.").withClass("text-muted mt-3")
                                                : div()
                                )
                        )
                )
                .render(response.getWriter());
    }

    private static String mapConfigScriptBody(List<MapPoint> mapPoints) throws Exception {
        String pointsJson = objectMapper.writeValueAsString(mapPoints).replace("</", "<\\/");
        return "window.yglCourseMapConfig = { mapPoints: " + pointsJson + " };";
    }

    private static String googleMapsScriptUrl(String googleMapsApiKey) {
        String encodedKey = URLEncoder.encode(googleMapsApiKey, StandardCharsets.UTF_8);
        return "https://maps.googleapis.com/maps/api/js?key=" + encodedKey + "&callback=yglInitCourseMap";
    }

        private static String fallbackMapUrl() {
                return "https://maps.google.com/maps?q="
                                + URLEncoder.encode("Yorkshire golf courses", StandardCharsets.UTF_8)
                                + "&z=8&output=embed";
        }

        public record MapPoint(String name, Double lat, Double lng, @Nullable String coursePath) {
    }
}
