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
public class HomePage implements View {

  @Override
  public @Nullable String getContentType() {
    return MediaType.TEXT_HTML_VALUE;
  }

  @Override
  public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

    Course featuredCourse = (Course) model.get("featuredCourse");

    @SuppressWarnings("unchecked")
    List<Course> allCourses = (List<Course>) model.get("courses");
    if (allCourses == null) {
      allCourses = List.of();
    }

    List<Region> regionOrder = List.of(
      Regions.NorthYorkshire,
      Regions.EastYorkshire,
      Regions.SouthYorkshire,
      Regions.WestYorkshire
    );

    Map<Region, Long> courseCountByRegion = allCourses.stream()
      .collect(Collectors.groupingBy(Course::region, Collectors.counting()));

    new YorkshireGolfPageTemplate()
      .withTitle("Yorkshire Golf Life")
      .withBody(
        // Hero section: randomly picked golf course
        div().withClass("bg-dark text-white py-5").with(
          div().withClass("container").with(
            featuredCourse != null ? p("Featured Course").withClass("text-uppercase text-secondary fw-semibold mb-1") : p(""),
            featuredCourse != null ? h1(featuredCourse.name()).withClass("display-4 fw-bold mb-2") : h1("Yorkshire Golf Life").withClass("display-4 fw-bold mb-2"),
            featuredCourse != null ? p(featuredCourse.region().displayName()).withClass("lead mb-4") : p(""),
            a("Explore all courses").withClass("btn btn-outline-light").withHref("/courses")
          )
        ),
        // Yorkshire Challenge section
        div().withClass("py-5 border-bottom").with(
          div().withClass("container").with(
            div().withClass("row align-items-center g-4").with(
              div().withClass("col-md-7").with(
                h2("The Yorkshire Challenge").withClass("fw-bold mb-3"),
                p("Can you play every golf course across the four ridings of Yorkshire? Track your progress through North, East, South and West Yorkshire and see how far along the challenge you are.").withClass("lead mb-4"),
                a("View the Challenge Tracker").withClass("btn btn-dark").withHref("/challenge")
              )
            )
          )
        ),
        // Golf courses in Yorkshire section
        div().withClass("py-5 bg-light").with(
          div().withClass("container").with(
            h2("Golf Courses in Yorkshire").withClass("fw-bold mb-2"),
            p("Yorkshire is home to an incredible variety of golf courses spread across its four ridings.").withClass("lead mb-5"),
            div().withClass("row row-cols-1 row-cols-md-2 row-cols-lg-4 g-4").with(
              regionOrder.stream().map(region ->
                div().withClass("col").with(
                  div().withClass("card h-100 text-center").with(
                    div().withClass("card-body").with(
                      h3(region.displayName()).withClass("h5 fw-bold card-title"),
                      p(courseCountByRegion.getOrDefault(region, 0L) + " courses").withClass("card-text text-muted")
                    ),
                    div().withClass("card-footer").with(
                      a("Browse courses").withClass("btn btn-sm btn-outline-dark").withHref("/courses")
                    )
                  )
                )
              ).toArray(j2html.tags.DomContent[]::new)
            )
          )
        )
      )
      .render(response.getWriter());

    log.info("HomePage rendered");

  }

}
