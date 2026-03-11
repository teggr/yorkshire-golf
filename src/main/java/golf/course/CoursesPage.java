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
import java.util.stream.Collectors;

import static j2html.TagCreator.*;

@Component
@Slf4j
public class CoursesPage implements View {

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

        Map<Region, List<Course>> byRegion = courses.stream()
                .collect(Collectors.groupingBy(Course::region));

        List<Region> regionOrder = List.of(
                Regions.NorthYorkshire,
                Regions.EastYorkshire,
                Regions.SouthYorkshire,
                Regions.WestYorkshire
        );

        new YorkshireGolfPageTemplate().withRequest(request)
                .withCurrentPageBasePath("/courses")
                .withTitle("Yorkshire Golf Courses")
                .withBody(
                        // Page header
                        div().withClass("ygl-page-header").with(
                                div().withClass("container").with(
                                        h1("Golf Courses").withClass("ygl-page-header__title"),
                                        p("Explore all " + courses.size() + " golf courses across the four ridings of Yorkshire.").withClass("ygl-page-header__lead")
                                )
                        ),
                        // Region navigation
                        nav().attr("aria-label", "Jump to region").withClass("ygl-region-nav").with(
                                div().withClass("container").with(
                                        regionOrder.stream().map(region ->
                                                a(region.displayName())
                                                        .withClass("ygl-btn ygl-btn--outline ygl-btn--lg")
                                                        .withHref("#" + toRegionSlug(region))
                                        ).toArray(j2html.tags.DomContent[]::new)
                                )
                        ),
                        // Course listings
                        coursesList(byRegion, regionOrder)
                )
                .render(response.getWriter());

        log.info("CoursesPage rendered");
    }

    static j2html.tags.DomContent coursesList(Map<Region, List<Course>> byRegion, List<Region> regionOrder) {
        return div().withClass("container ygl-page").with(
                div().with(
                        regionOrder.stream().map(region -> {
                            List<Course> regionCourses = byRegion.getOrDefault(region, List.of());
                            return div().withClass("mb-5").withId(toRegionSlug(region)).with(
                                    div().withClass("ygl-region-header").with(
                                            h2(region.displayName()).withClass("ygl-region-header__title"),
                                            span(regionCourses.size() + " courses").withClass("ygl-region-header__count")
                                    ),
                                    div().withClass("row row-cols-1 row-cols-md-2 row-cols-lg-3 g-3").with(
                                            regionCourses.stream()
                                                    .map(course -> div().withClass("col").with(courseCard(course)))
                                                    .toArray(j2html.tags.DomContent[]::new)
                                    )
                            );
                        }).toArray(j2html.tags.DomContent[]::new)
                )
        );
    }

    static j2html.tags.DomContent courseCard(Course course) {
        return courseCard(course, course.mainImageUrl());
    }

    static j2html.tags.DomContent courseCard(Course course, @Nullable String imageUrl) {
        String thumbImageUrl = CourseImages.toThumbUrl(imageUrl);
        String fallbackAttr = "this.onerror=null;this.src='" + imageUrl + "';";
        j2html.tags.DomContent mediaEl = div().withClass("ygl-card__media").with(
                imageUrl != null && !imageUrl.isEmpty()
                        ? img().withSrc(thumbImageUrl)
                                .withAlt(course.name())
                                .withClass("ygl-card__img")
                                .attr("loading", "lazy")
                                .attr("decoding", "async")
                                .attr("onerror", fallbackAttr)
                        : div("No image available").withClass("ygl-card__placeholder").attr("aria-hidden", "true")
        );

        j2html.tags.DomContent cardBody = div().withClass("ygl-card__body").with(
                p(course.name()).withClass("ygl-card__text mb-2"),
                course.playAndStay()
                        ? p().withClass("mb-0 d-flex align-items-center gap-2").with(
                                span().attr("role", "img")
                                        .attr("title", "Play & Stay – onsite accommodation available")
                                        .attr("aria-label", "Play & Stay – onsite accommodation available")
                                        .with(i().withClass("bi bi-house-door-fill ygl-badge--play-and-stay").attr("aria-hidden", "true"))
                          )
                        : span()
        );

        String courseUrl = "/courses/" + Courses.toCourseSlug(course.name());

        return a().withHref(courseUrl).withClass("ygl-card ygl-card--course h-100 text-decoration-none").with(
                mediaEl,
                cardBody
        );
    }

        static String toRegionSlug(Region region) {
                return region.displayName().toLowerCase().replace(" ", "-");
        }

}
