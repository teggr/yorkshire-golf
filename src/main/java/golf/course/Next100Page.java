package golf.course;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.View;

import golf.web.YorkshireGolfPageTemplate;

import java.util.List;
import java.util.Map;

import static j2html.TagCreator.*;

@Component
@Slf4j
public class Next100Page implements View {

    @Override
    public @Nullable String getContentType() {
        return MediaType.TEXT_HTML_VALUE;
    }

    @Override
    public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
                response.setContentType(MediaType.TEXT_HTML_VALUE);
                response.setCharacterEncoding("UTF-8");

        @SuppressWarnings("unchecked")
        List<Course> courses = (List<Course>) model.get("courses");

        new YorkshireGolfPageTemplate()
                .withRequest(request)
                .withCurrentPageBasePath("/next-100")
                .withTitle("Next 100 – Yorkshire Golf")
                .withDescription("Explore Yorkshire's standout golf courses featured in Golf Monthly's UK & Ireland rankings 101–200. Discover hidden gems and exceptional clubs across all four ridings.")
                .withBody(
                        // Page header
                        div().withClass("ygl-page-header").with(
                                div().withClass("container").with(
                                        span("Golf Monthly Next 100").withClass("ygl-hero__label"),
                                        h1("Yorkshire Courses 101–200").withClass("ygl-page-header__title"),
                                        p("Yorkshire's standout courses in Golf Monthly's UK & Ireland rankings 101–200.").withClass("ygl-page-header__lead")
                                )
                        ),
                        // Course list
                        div().withClass("container ygl-page").with(
                                courses.isEmpty()
                                        ? p("No Next 100 courses found.").withClass("text-muted")
                                        : div().withClass("ygl-top100-list").with(
                                        courses.stream()
                                                .map(Next100Page::courseSection)
                                                .toArray(j2html.tags.DomContent[]::new)
                                )
                        )
                )
                .render(response.getWriter());

        log.info("Next100Page rendered");
    }

    static j2html.tags.DomContent courseSection(Course course) {
                if (!Boolean.TRUE.equals(course.next100())) {
            return span();
        }
        return div().withClass("ygl-top100-entry").with(
                div().withClass("ygl-top100-entry__rank").with(
                        span("Golf Monthly Next 100").withClass("ygl-top100-entry__rank-label")
                ),
                course.mainImageUrl() != null && !course.mainImageUrl().isEmpty()
                        ? div().withClass("ygl-top100-entry__image-wrapper").with(
                                img().withSrc(course.mainImageUrl())
                                        .withAlt(course.name())
                                        .withClass("ygl-top100-entry__image")
                        )
                        : div().withClass("ygl-top100-entry__image-wrapper ygl-top100-entry__image-wrapper--empty").with(
                                div("No image available").withClass("ygl-card__placeholder")
                        ),
                div().withClass("ygl-top100-entry__body").with(
                        div().withClass("container").with(
                                h2(course.name()).withClass("ygl-top100-entry__title"),
                                p(course.region().displayName()).withClass("ygl-top100-entry__region"),
                                course.website() != null && !course.website().isEmpty()
                                        ? a().withClass("ygl-btn ygl-btn--primary")
                                                .withHref(course.website())
                                                .withTarget("_blank")
                                                .withRel("noopener noreferrer")
                                                .with(
                                                        text("Visit website"),
                                                        i().withClass("bi bi-box-arrow-up-right")
                                                )
                                        : span()
                        )
                )
        );
    }

}
