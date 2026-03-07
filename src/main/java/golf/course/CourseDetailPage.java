package golf.course;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.View;

import golf.web.YorkshireGolfPageTemplate;

import java.util.Map;

import static j2html.TagCreator.*;

@Component
@Slf4j
public class CourseDetailPage implements View {

    @Override
    public @Nullable String getContentType() {
        return MediaType.TEXT_HTML_VALUE;
    }

    @Override
    public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
                response.setContentType(MediaType.TEXT_HTML_VALUE);
                response.setCharacterEncoding("UTF-8");

        Course course = (Course) model.get("course");

        new YorkshireGolfPageTemplate()
                .withTitle(course.name() + " - Yorkshire Golf Life")
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
                                        div().withClass("row justify-content-center").with(
                                                div().withClass("col-lg-8").with(
                                                        courseDetail(course)
                                                )
                                        )
                                )
                        )
                )
                .render(response.getWriter());

        log.info("CourseDetailPage rendered for course: {}", course.name());
    }

    static j2html.tags.DomContent courseDetail(Course course) {
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
                courseInfoSection(course)
        );
    }

    private static j2html.tags.DomContent courseInfoSection(Course course) {
        return div().withClass("ygl-article__content").with(
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
        );
    }

}
