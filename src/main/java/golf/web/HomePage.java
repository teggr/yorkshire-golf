package golf.web;

import golf.course.Courses;
import golf.course.Course;
import golf.course.Region;
import golf.course.Regions;
import golf.utils.security.CsrfUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.View;

import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
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

    RandomGenerator random = RandomGenerator.getDefault();

    Map<Region, List<Course>> coursesByRegion = allCourses.stream()
      .collect(Collectors.groupingBy(Course::region));

    Map<Region, Course> featuredCourseByRegion = regionOrder.stream()
      .collect(Collectors.toMap(
        region -> region,
        region -> {
          List<Course> regionCourses = coursesByRegion.getOrDefault(region, List.of());
          if (regionCourses.isEmpty()) {
            return null;
          }
          return regionCourses.get(random.nextInt(regionCourses.size()));
        }
      ));

    long totalCourses = allCourses.size();

    new YorkshireGolfPageTemplate()
      .withRequest(request)
      .withTitle("Yorkshire Golf Life")
      .withBody(
        // Hero
        div().withClass("ygl-hero").with(
          div().withClass("container").with(
            div().withClass("row g-4 align-items-center").with(
              // Text Content Column
              div().withClass("col-12 col-lg-7 order-1").with(
                featuredCourse != null
                  ? div().with(
                      span("Featured Course").withClass("ygl-hero__label"),
                      h1(featuredCourse.name()).withClass("ygl-hero__title"),
                      p(featuredCourse.region().displayName()).withClass("ygl-hero__subtitle"),
                      a().withHref("/courses/" + Courses.toCourseSlug(featuredCourse.name()))
                          .withClass("ygl-btn ygl-btn--primary ygl-btn--lg")
                          .with(text("View course"))
                    )
                  : div().with(
                      h1("Yorkshire Golf Life").withClass("ygl-hero__title"),
                      p("Tracking the journey to play every golf course across Yorkshire.").withClass("ygl-hero__subtitle")
                    )
              ),
              // Image Column
              featuredCourse != null && featuredCourse.mainImageUrl() != null && !featuredCourse.mainImageUrl().isEmpty()
                ? div().withClass("col-12 col-lg-5 order-2").with(
                    div().withClass("ygl-hero__image-wrapper").with(
                      img().withSrc(featuredCourse.mainImageUrl())
                          .withAlt(featuredCourse.name())
                          .withClass("ygl-hero__image")
                    )
                  )
                : text("")
            )
          )
        ),

        // Stat strip
        div().withClass("ygl-stat-strip").with(
          div().withClass("container").with(
            div().withClass("row g-3 row-cols-2 row-cols-lg-4").with(
              regionOrder.stream().map(region ->
                div().withClass("col").with(
                  div().withClass("ygl-card ygl-card--stat").with(
                    div().withClass("ygl-card__body").with(
                      span(String.valueOf(courseCountByRegion.getOrDefault(region, 0L))).withClass("ygl-card__number"),
                      p(region.displayName()).withClass("ygl-card__label")
                    )
                  )
                )
              ).toArray(j2html.tags.DomContent[]::new)
            )
          )
        ),

        // Explore All Courses CTA
        div().withClass("ygl-section pb-0 text-center").with(
          div().withClass("container").with(
            a("Explore All Courses →").withClass("ygl-btn ygl-btn--accent-light ygl-btn--xl").withHref("/courses")
          )
        ),

        // Login / account section
        buildAccountSection(request),

        // Yorkshire Challenge feature
        div().withClass("ygl-section").with(
          div().withClass("container").with(
            div().withClass("ygl-feature").with(
              div().withClass("row align-items-center g-4").with(
                div().withClass("col-lg-7").with(
                  span("The Challenge").withClass("ygl-feature__eyebrow"),
                  h2("The Yorkshire Golf Challenge").withClass("ygl-feature__title"),
                  p("Can you play every golf course across the four ridings of Yorkshire? Track your progress through North, East, South and West Yorkshire and see how far along the challenge you are.").withClass("ygl-feature__text"),
                  a("View the Challenge Tracker →").withClass("ygl-btn ygl-btn--dark").withHref("/challenge")
                ),
                div().withClass("col-lg-4 offset-lg-1 text-center").with(
                  div().with(
                    span(String.valueOf(totalCourses)).withClass("d-block ygl-feature__stat"),
                    span("courses across Yorkshire").withClass("d-block ygl-feature__stat-label")
                  )
                )
              )
            )
          )
        ),

        // Courses by region
        div().withClass("ygl-section ygl-section--alt").with(
          div().withClass("container").with(
            h2("Courses by Region").withClass("ygl-section__title"),
            p("Yorkshire is home to an incredible variety of golf courses spread across its four historic ridings.").withClass("ygl-section__subtitle"),
            div().withClass("row row-cols-1 row-cols-md-2 row-cols-lg-4 g-4").with(
              regionOrder.stream().map(region -> {
                Course regionCourse = featuredCourseByRegion.get(region);
                return div().withClass("col").with(
                  div().withClass("ygl-card h-100").with(
                    div().withClass("ygl-card__body").with(
                      h3(region.displayName()).withClass("ygl-card__title"),
                      div().withClass("ygl-card__media mb-3").with(
                        regionCourse != null && regionCourse.mainImageUrl() != null && !regionCourse.mainImageUrl().isEmpty()
                          ? img().withSrc(regionCourse.mainImageUrl())
                              .withAlt(regionCourse.name())
                              .withClass("ygl-card__img")
                          : div("No image available").withClass("ygl-card__placeholder").attr("aria-hidden", "true")
                      ),
                      regionCourse != null
                        ? p(regionCourse.name()).withClass("ygl-card__text mb-2")
                        : p("No courses currently available.").withClass("ygl-card__text mb-2"),
                      regionCourse != null
                        ? p().withClass("mb-0").with(
                            a().withClass("ygl-btn ygl-btn--primary ygl-btn--sm")
                              .withHref("/courses/" + Courses.toCourseSlug(regionCourse.name()))
                              .with(text("View course"))
                          )
                        : text("")
                    ),
                    div().withClass("ygl-card__footer").with(
                      a("Browse →").withClass("ygl-btn ygl-btn--outline ygl-btn--sm").withHref("/courses#" + toRegionSlug(region))
                    )
                  )
                );
              }).toArray(j2html.tags.DomContent[]::new)
            )
          )
        )
      )
      .render(response.getWriter());

    log.info("HomePage rendered");

  }

  static String toRegionSlug(Region region) {
    return region.displayName().toLowerCase().replace(" ", "-");
  }

  private static j2html.tags.DomContent buildAccountSection(HttpServletRequest request) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    boolean loggedIn = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());

    if (loggedIn) {
      return div().withClass("ygl-section pb-0").with(
        div().withClass("container").with(
          div().withClass("text-center").with(
            h2("Welcome back!").withClass("ygl-feature__title mb-3"),
            p("Manage your rounds and track your Yorkshire Golf Challenge progress.").withClass("mb-3"),
            a("My Rounds →").withClass("ygl-btn ygl-btn--primary me-3").withHref("/my-rounds"),
            a("My Tracker →").withClass("ygl-btn ygl-btn--dark").withHref("/challenge")
          )
        )
      );
    }

    j2html.tags.DomContent csrfField = CsrfUtil.csrfInput(request);
    return div().withClass("ygl-section pb-0").with(
      div().withClass("container").with(
        div().withClass("row justify-content-center").with(
          div().withClass("col-md-8 col-lg-5").with(
            h2("Sign in to track your challenge").withClass("ygl-feature__title mb-3 text-center"),
            form().withMethod("post").withAction("/login").with(
              csrfField,
              div().withClass("mb-3").with(
                label("Email address").withFor("hp-email").withClass("form-label"),
                input().withType("email").withId("hp-email").withName("username")
                  .withClass("form-control").attr("required", "")
              ),
              div().withClass("mb-3").with(
                label("Password").withFor("hp-password").withClass("form-label"),
                input().withType("password").withId("hp-password").withName("password")
                  .withClass("form-control").attr("required", "")
              ),
              button("Sign In").withType("submit").withClass("btn ygl-btn ygl-btn--primary w-100 mb-2")
            ),
            div().withClass("text-center").with(
              p().with(
                a("Create an account").withHref("/register"),
                text(" · "),
                a("Forgot password?").withHref("/forgot-password")
              )
            )
          )
        )
      )
    );
  }

}
