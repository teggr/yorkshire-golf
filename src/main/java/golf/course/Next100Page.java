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

        @SuppressWarnings("unchecked")
        List<Course> courses = (List<Course>) model.get("courses");

        new YorkshireGolfPageTemplate()
                .withTitle("Next 100 – Yorkshire Golf")
                .withBody(
                        // Page header
                        div().withClass("ygl-page-header").with(
                                div().withClass("container").with(
                                        h1("The Next 100").withClass("ygl-page-header__title"),
                                        p("Yorkshire's finest in the Golf Monthly 101–200.").withClass("ygl-page-header__lead")
                                )
                        ),
                        // Introduction
                        div().withClass("ygl-section").with(
                                div().withClass("container").with(
                                        div().withClass("row justify-content-center").with(
                                                div().withClass("col-lg-8 text-center").with(
                                                        p("Golf Monthly's extended top 200 recognises the very best golf courses across the UK and Ireland. " +
                                                                "Positions 101 to 200 – known as the Next 100 – celebrate courses of exceptional quality that sit just " +
                                                                "outside the illustrious top 100. Yorkshire punches well above its weight in this prestigious ranking, " +
                                                                "with a remarkable collection of courses that showcase the county's outstanding golfing heritage. " +
                                                                "Each of these clubs offers a world-class experience, exceptional course design, and the warm welcome " +
                                                                "that Yorkshire golf is renowned for.").withClass("lead")
                                                )
                                        )
                                )
                        ),
                        // Course sections
                        courses.isEmpty()
                                ? div().withClass("container ygl-page").with(
                                        p("No Next 100 courses found.").withClass("text-muted text-center py-5")
                                )
                                : div().with(
                                        courses.stream()
                                                .map(Next100Page::courseSection)
                                                .toArray(j2html.tags.DomContent[]::new)
                                )
                )
                .render(response.getWriter());

        log.info("Next100Page rendered");
    }

    static j2html.tags.DomContent courseSection(Course course) {
        boolean hasImage = course.mainImageUrl() != null && !course.mainImageUrl().isEmpty();
        int rank = course.next100() != null ? course.next100() : 0;

        return div().withClass("ygl-next100-section").with(
                div().withClass("ygl-next100-section__inner").with(
                        // Image panel
                        hasImage
                                ? div().withClass("ygl-next100-section__image").with(
                                        img().withSrc(course.mainImageUrl())
                                                .withAlt(course.name())
                                                .withClass("ygl-next100-section__img")
                                )
                                : div().withClass("ygl-next100-section__image ygl-next100-section__image--placeholder"),
                        // Content panel
                        div().withClass("ygl-next100-section__content").with(
                                span("#" + rank).withClass("ygl-next100-section__rank"),
                                h2(course.name()).withClass("ygl-next100-section__title"),
                                p("Golf Monthly UK & Ireland Top 200 · Ranked " + rank + " in the Next 100").withClass("ygl-next100-section__meta"),
                                p(course.region().displayName()).withClass("ygl-next100-section__region"),
                                course.website() != null && !course.website().isEmpty()
                                        ? a().withClass("ygl-btn ygl-btn--primary").withHref(course.website())
                                                .withTarget("_blank")
                                                .withRel("noopener noreferrer")
                                                .with(
                                                        text("Visit website"),
                                                        i().withClass("bi bi-box-arrow-up-right ms-2")
                                                )
                                        : span()
                        )
                )
        );
    }

}
