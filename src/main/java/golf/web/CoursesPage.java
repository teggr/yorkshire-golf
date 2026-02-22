package golf.web;

import golf.course.Course;
import golf.course.Region;
import golf.course.Regions;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.View;

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
                        div().withClass("container py-4").with(
                                h1("Yorkshire Golf Courses").withClass("display-6 mb-2"),
                                p("Explore all the golf courses across the four ridings of Yorkshire.").withClass("lead mb-5"),
                                div().with(
                                        regionOrder.stream().map(region -> {
                                            List<Course> regionCourses = byRegion.getOrDefault(region, List.of());
                                            return div().withClass("mb-5").with(
                                                    h2(region.displayName()).withClass("h4 fw-bold border-bottom pb-2 mb-3"),
                                                    div().withClass("row row-cols-1 row-cols-md-2 row-cols-lg-3 g-3").with(
                                                            regionCourses.stream()
                                                                    .map(course -> div().withClass("col").with(
                                                                            div().withClass("card h-100").with(
                                                                                    div().withClass("card-body").with(
                                                                                            p(course.name()).withClass("card-text mb-0")
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
