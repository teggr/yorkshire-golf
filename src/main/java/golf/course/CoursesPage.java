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

        new YorkshireGolfPageTemplate()
                .withTitle("Yorkshire Golf Courses")
                .withBody(
                        // Page header
                        div().withClass("ygl-page-header").with(
                                div().withClass("container").with(
                                        h1("Golf Courses").withClass("ygl-page-header__title"),
                                        p("Explore all " + courses.size() + " golf courses across the four ridings of Yorkshire.").withClass("ygl-page-header__lead")
                                )
                        ),
                        // Course listings
                        div().withClass("container ygl-page").with(
                                div().with(
                                        regionOrder.stream().map(region -> {
                                            List<Course> regionCourses = byRegion.getOrDefault(region, List.of());
                                            return div().withClass("mb-5").with(
                                                    div().withClass("ygl-region-header").with(
                                                            h2(region.displayName()).withClass("ygl-region-header__title"),
                                                            span(regionCourses.size() + " courses").withClass("ygl-region-header__count")
                                                    ),
                                                    div().withClass("row row-cols-1 row-cols-md-2 row-cols-lg-3 g-3").with(
                                                            regionCourses.stream()
                                                                    .map(course -> div().withClass("col").with(
                                                                            course.website() != null && !course.website().isEmpty()
                                                                                ? a().withHref(course.website())
                                                                                    .withTarget("_blank")
                                                                                    .withRel("noopener noreferrer")
                                                                                    .withClass("ygl-card-link").with(
                                                                                        div().withClass("ygl-card ygl-card--course h-100").with(
                                                                                                div().withClass("ygl-card__body").with(
                                                                                                        p(course.name()).withClass("ygl-card__text mb-2"),
                                                                                                        p(course.website()).withClass("ygl-card__url text-muted small mb-0")
                                                                                                )
                                                                                        )
                                                                                )
                                                                                : div().withClass("ygl-card ygl-card--course h-100").with(
                                                                                        div().withClass("ygl-card__body").with(
                                                                                                p(course.name()).withClass("ygl-card__text mb-2")
                                                                                        )
                                                                                )
                                                                    ))
                                                                    .toArray(j2html.tags.DomContent[]::new)
                                                    )
                                            );
                                        }).toArray(j2html.tags.DomContent[]::new)
                                )
                        )
                )
                .render(response.getWriter());

        log.info("CoursesPage rendered");
    }

}
