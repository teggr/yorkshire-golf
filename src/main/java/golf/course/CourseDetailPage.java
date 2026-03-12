package golf.course;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.View;

import golf.web.YorkshireGolfPageTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static j2html.TagCreator.*;

@Component
@Slf4j
public class CourseDetailPage implements View {

        @Value("${GOOGLE_MAPS_API_KEY:}")
        private String googleMapsApiKey;

    @Override
    public @Nullable String getContentType() {
        return MediaType.TEXT_HTML_VALUE;
    }

    @Override
    public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
                response.setContentType(MediaType.TEXT_HTML_VALUE);
                response.setCharacterEncoding("UTF-8");

        Course course = (Course) model.get("course");
        List<Course> nearbyCourses = resolveNearbyCourses(model);

        new YorkshireGolfPageTemplate()
                .withRequest(request)
                .withCurrentPageBasePath("/courses")
                .withTitle(course.name() + " - Yorkshire Golf Life")
                .withDescription(course.name() + " is a golf club in " + course.region().displayName() + ". Find course information, location details, and nearby courses on Yorkshire Golf Life.")
                .withOgImage(course.mainImageUrl() != null && !course.mainImageUrl().isEmpty() ? course.mainImageUrl() : null)
                .withOgType("article")
                .withBody(
                        // Hero image
                        course.mainImageUrl() != null && !course.mainImageUrl().isEmpty()
                                ? div().withClass("ygl-course-hero").with(
                                        img().withSrc(course.mainImageUrl())
                                                .withAlt(course.name())
                                                .withClass("ygl-course-hero__image")
                                  )
                                : text(""),

                        // Page content
                        div().withClass("container ygl-page").with(
                                div().withClass("ygl-page__content").with(
                                        a("← Back to Courses").withHref("/courses").withClass("ygl-back-link"),
                                        courseDetail(course, googleMapsApiKey, nearbyCourses)
                                )
                        )
                )
                .render(response.getWriter());

        log.info("CourseDetailPage rendered for course: {}", course.name());
    }

    static j2html.tags.DomContent courseDetail(Course course) {
        return courseDetail(course, "");
    }

    static j2html.tags.DomContent courseDetail(Course course, String googleMapsApiKey) {
                return courseDetail(course, googleMapsApiKey, List.of());
        }

        static j2html.tags.DomContent courseDetail(Course course, String googleMapsApiKey, List<Course> nearbyCourses) {
        return article().withClass("ygl-article").with(
                h1(course.name()).withClass("ygl-article__title"),
                p().withClass("ygl-article__meta").with(
                        span(course.region().displayName()).withClass("ygl-badge"),
                        course.playAndStay()
                                ? span().withClass("ms-2").with(
                                        i().withClass("bi bi-house-door-fill ygl-badge--play-and-stay me-1").attr("aria-hidden", "true"),
                                        text("Play & Stay")
                                  )
                                : span()
                ),
                courseInfoSection(course, googleMapsApiKey, nearbyCourses)
        );
    }

    private static j2html.tags.DomContent courseInfoSection(Course course, String googleMapsApiKey, List<Course> nearbyCourses) {
        return div().withClass("ygl-article__content").with(
                div().withClass("row g-4 ygl-course-overview").with(
                        div().withClass("col-lg-7").with(
                // Website link
                course.website() != null && !course.website().isEmpty()
                        ? div().withClass("mb-3").with(
                                a().withClass("ygl-btn ygl-btn--primary")
                                        .withHref(course.website())
                                        .withTarget("_blank")
                                        .withRel("noopener noreferrer")
                                        .with(
                                                text("Visit website"),
                                                i().withClass("bi bi-box-arrow-up-right ms-2")
                                        )
                          )
                        : text(""),

                // Course details table
                div().withClass("ygl-course-details").with(
                        dl().withClass("row").with(
                                dt().withClass("col-sm-4").with(text("Region")),
                                dd().withClass("col-sm-8").with(text(course.region().displayName())),

                                course.address() != null && !course.address().isEmpty()
                                        ? join(
                                        dt().withClass("col-sm-4").with(text("Address")),
                                        dd().withClass("col-sm-8").with(text(course.address()))
                                )
                                        : text(""),

                                course.playAndStay()
                                        ? join(
                                                dt().withClass("col-sm-4").with(text("Play & Stay")),
                                                dd().withClass("col-sm-8").with(
                                                        i().withClass("bi bi-house-door-fill ygl-badge--play-and-stay me-1").attr("aria-hidden", "true"),
                                                        text("Onsite accommodation available")
                                                )
                                          )
                                        : text(""),

                                course.website() != null && !course.website().isEmpty()
                                        ? join(
                                                dt().withClass("col-sm-4").with(text("Website")),
                                                dd().withClass("col-sm-8").with(
                                                        a(course.website())
                                                                .withHref(course.website())
                                                                .withTarget("_blank")
                                                                .withRel("noopener noreferrer")
                                                )
                                          )
                                        : text("")
                        )
                )
                        ),

                        div().withClass("col-lg-5").with(

                course.lat() != null && course.lng() != null
                        ? div().withClass("ygl-course-map").with(
                                h2("Google Maps").withClass("ygl-course-map__title"),
                                div().withClass("ratio ratio-16x9 ygl-course-map__embed").with(
                                        iframe()
                                                .withSrc(googleMapsEmbedUrl(course, googleMapsApiKey))
                                                .withTitle(course.name() + " map")
                                                .attr("style", "border:0;")
                                                .attr("loading", "lazy")
                                                .attr("referrerpolicy", "no-referrer-when-downgrade")
                                                .attr("allowfullscreen", "")
                                )
                        )
                        : text("")
                        )
                ),

                nearbyCoursesSection(nearbyCourses)
        );
    }

    private static j2html.tags.DomContent nearbyCoursesSection(List<Course> nearbyCourses) {
        if (nearbyCourses == null || nearbyCourses.isEmpty()) {
            return text("");
        }

        return section().withClass("ygl-course-nearby").with(
                h2("Nearby courses").withClass("ygl-course-nearby__title"),
                div().withClass("row row-cols-1 row-cols-md-2 row-cols-lg-3 g-3").with(
                        nearbyCourses.stream()
                                .map(nearbyCourse -> div().withClass("col").with(nearbyCourseCard(nearbyCourse)))
                                .toArray(j2html.tags.DomContent[]::new)
                )
        );
    }

    private static j2html.tags.DomContent nearbyCourseCard(Course nearbyCourse) {
        String courseUrl = "/courses/" + Courses.toCourseSlug(nearbyCourse.name());

        return div().withClass("ygl-card h-100").with(
                div().withClass("ygl-card__body").with(
                        div().withClass("ygl-card__media mb-3").with(
                                nearbyCourse.mainImageUrl() != null && !nearbyCourse.mainImageUrl().isEmpty()
                                        ? img().withSrc(nearbyCourse.mainImageUrl())
                                                .withAlt(nearbyCourse.name())
                                                .withClass("ygl-card__img")
                                        : div("No image available").withClass("ygl-card__placeholder").attr("aria-hidden", "true")
                        ),
                        p(nearbyCourse.name()).withClass("ygl-card__text mb-3"),
                        p().withClass("mb-0 mt-auto").with(
                                a().withClass("ygl-btn ygl-btn--primary ygl-btn--sm")
                                        .withHref(courseUrl)
                                        .with(text("View course"))
                        )
                )
        );
    }

    private static List<Course> resolveNearbyCourses(@Nullable Map<String, ?> model) {
        if (model == null) {
            return List.of();
        }

        Object nearbyCoursesModel = model.get("nearbyCourses");
        if (!(nearbyCoursesModel instanceof List<?> rawList)) {
            return List.of();
        }

        return rawList.stream()
                .filter(Course.class::isInstance)
                .map(Course.class::cast)
                .toList();
    }

    private static String googleMapsEmbedUrl(Course course, String googleMapsApiKey) {
        String query = course.address() != null && !course.address().isBlank()
                ? course.name() + ", " + course.address()
                : course.lat() + "," + course.lng();
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

        if (googleMapsApiKey == null || googleMapsApiKey.isBlank()) {
            return "https://maps.google.com/maps?q=" + encodedQuery + "&z=14&output=embed";
        }

        return "https://www.google.com/maps/embed/v1/place?key=" +
               URLEncoder.encode(googleMapsApiKey, StandardCharsets.UTF_8) +
               "&q=" + encodedQuery;
    }

}
