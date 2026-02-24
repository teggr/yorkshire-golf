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
        // Hero section
        div().withClass("ygl-hero").with(
          div().withClass("container").with(
            featuredCourse != null ? p("Featured Course").withClass("ygl-hero__label") : p(""),
            featuredCourse != null ? h1(featuredCourse.name()).withClass("ygl-hero__title") : h1("Yorkshire Golf Life").withClass("ygl-hero__title"),
            featuredCourse != null ? p(featuredCourse.region().displayName()).withClass("ygl-hero__subtitle") : p(""),
            a("Explore all courses").withClass("ygl-btn ygl-btn--outline-light").withHref("/courses")
          )
        ),
        // Yorkshire Challenge section
        div().withClass("ygl-section").with(
          div().withClass("container").with(
            div().withClass("row align-items-center g-4").with(
              div().withClass("col-md-7").with(
                h2("The Yorkshire Challenge").withClass("ygl-section__title"),
                p("Can you play every golf course across the four ridings of Yorkshire? Track your progress through North, East, South and West Yorkshire and see how far along the challenge you are.").withClass("ygl-section__subtitle"),
                a("View the Challenge Tracker").withClass("ygl-btn ygl-btn--primary").withHref("/challenge")
              )
            )
          )
        ),
        // Golf courses in Yorkshire section
        div().withClass("ygl-section ygl-section--shaded").with(
          div().withClass("container").with(
            h2("Golf Courses in Yorkshire").withClass("ygl-section__title"),
            p("Yorkshire is home to an incredible variety of golf courses spread across its four ridings.").withClass("ygl-section__subtitle"),
            div().withClass("row row-cols-1 row-cols-md-2 row-cols-lg-4 g-4").with(
              regionOrder.stream().map(region ->
                div().withClass("col").with(
                  div().withClass("ygl-card h-100 text-center").with(
                    div().withClass("ygl-card__body").with(
                      h3(region.displayName()).withClass("h5 ygl-card__title"),
                      p(courseCountByRegion.getOrDefault(region, 0L) + " courses").withClass("ygl-card__text text-muted")
                    ),
                    div().withClass("ygl-card__footer").with(
                      a("Browse courses").withClass("ygl-btn ygl-btn--outline ygl-btn--sm").withHref("/courses")
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
