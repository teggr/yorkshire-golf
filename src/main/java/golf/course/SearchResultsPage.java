package golf.course;

import golf.web.YorkshireGolfPageTemplate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.View;

import java.util.List;
import java.util.Map;

import static j2html.TagCreator.*;

@Component
@Slf4j
public class SearchResultsPage implements View {

    @Override
    public @Nullable String getContentType() {
        return MediaType.TEXT_HTML_VALUE;
    }

    @Override
    public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.setCharacterEncoding("UTF-8");

        String query = model != null && model.get("query") != null ? String.valueOf(model.get("query")) : "";

        @SuppressWarnings("unchecked")
        List<Course> results = model != null && model.get("results") != null
                ? (List<Course>) model.get("results")
                : List.of();

        int resultCount = results.size();

        new YorkshireGolfPageTemplate().withRequest(request)
                .withCurrentPageBasePath("/courses")
                .withTitle("Course Search Results")
                .withBody(
                        div().withClass("ygl-page-header").with(
                                div().withClass("container").with(
                                        h1("Search Results").withClass("ygl-page-header__title"),
                                        p(resultCount + " matches for \"" + query + "\"").withClass("ygl-page-header__lead")
                                )
                        ),
                        div().withClass("container ygl-page").with(
                                buildSearchForm(query),
                                resultCount == 0
                                        ? zeroState()
                                        : div().withClass("row row-cols-1 row-cols-md-2 row-cols-lg-3 g-3").with(
                                        results.stream()
                                                .map(course -> div().withClass("col").with(searchResultCard(course)))
                                                .toArray(j2html.tags.DomContent[]::new)
                                )
                        )
                )
                .render(response.getWriter());

        log.info("SearchResultsPage rendered");
    }

    private static j2html.tags.DomContent buildSearchForm(String query) {
        return div().withClass("ygl-search-panel mb-4").with(
                form().withAction("/search").withMethod("get").withClass("ygl-search-form").with(
                        label("Search Courses").attr("for", "course-search-q").withClass("visually-hidden"),
                        input().withType("search")
                                .withName("q")
                                .withId("course-search-q")
                                .withClass("form-control")
                                .withPlaceholder("Search by course name or address")
                                .withValue(query),
                        button("Search").withType("submit").withClass("ygl-btn ygl-btn--dark")
                )
        );
    }

    private static j2html.tags.DomContent zeroState() {
        return div().withClass("ygl-search-zero-state").with(
                h2("No courses matched your search").withClass("ygl-section__title"),
                p("Try another name or location, or browse every course in Yorkshire.").withClass("ygl-section__subtitle"),
                a("Explore All Courses →").withClass("ygl-btn ygl-btn--accent-light ygl-btn--xl").withHref("/courses")
        );
    }

    private static j2html.tags.DomContent searchResultCard(Course course) {
        String courseUrl = "/courses/" + Courses.toCourseSlug(course.name());
        String imageUrl = course.mainImageUrl();

        return div().withClass("ygl-card ygl-card--course h-100").with(
                div().withClass("ygl-card__media").with(
                        imageUrl != null && !imageUrl.isEmpty()
                                ? img().withSrc(imageUrl).withAlt(course.name()).withClass("ygl-card__img")
                                : div("No image available").withClass("ygl-card__placeholder").attr("aria-hidden", "true")
                ),
                div().withClass("ygl-card__body").with(
                        p(course.name()).withClass("ygl-card__text mb-2"),
                        course.playAndStay()
                                ? p().withClass("mb-0 d-flex align-items-center gap-2").with(
                                span().attr("role", "img")
                                        .attr("title", "Play & Stay – onsite accommodation available")
                                        .attr("aria-label", "Play & Stay – onsite accommodation available")
                                        .with(i().withClass("bi bi-house-door-fill ygl-badge--play-and-stay").attr("aria-hidden", "true"))
                        )
                                : span()
                ),
                div().withClass("ygl-card__footer").with(
                        a("View course").withClass("ygl-btn ygl-btn--primary ygl-btn--sm").withHref(courseUrl)
                )
        );
    }
}
