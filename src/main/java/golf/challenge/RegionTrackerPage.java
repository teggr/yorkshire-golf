package golf.challenge;

import golf.course.Course;
import golf.course.Regions;
import golf.utils.security.CsrfUtil;
import golf.web.YorkshireGolfPageTemplate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.View;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static golf.utils.chartjs.ChartJsCreator.*;
import static j2html.TagCreator.*;

@Component
@Slf4j
public class RegionTrackerPage implements View {

  @Override
  public @Nullable String getContentType() {
    return MediaType.TEXT_HTML_VALUE;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
    response.setContentType(MediaType.TEXT_HTML_VALUE);
    response.setCharacterEncoding("UTF-8");

    RegionChallengeTracker tracker = (RegionChallengeTracker) model.get("tracker");
    String trackerId = (String) model.get("trackerId");
    boolean canAddRound = Boolean.TRUE.equals(model.get("canAddRound"));
    List<Course> allCourses = model.get("allCourses") != null ? (List<Course>) model.get("allCourses") : List.of();
    String errorMessage = (String) model.get("error");
    String successMessage = (String) model.get("success");
    j2html.tags.DomContent csrfField = CsrfUtil.csrfInput(request);

    Map<String, Object> chartData = new HashMap<>();
    chartData.put("overall_percentage",
      tracker.overallProgress()
    );
    chartData.put("overall_progress", List.of(
      tracker.totalCoursesPlayed(),
      tracker.totalCoursesToBePlayed()
    ));
    chartData.put("total_played", List.of(
      tracker.totalCoursesPlayed()
    ));
    chartData.put("total_course_count", List.of(
      tracker.totalCourseCount()
    ));
    chartData.put("region_played", List.of(
      tracker.totalCoursesPlayed(Regions.NorthYorkshire),
      tracker.totalCoursesPlayed(Regions.EastYorkshire),
      tracker.totalCoursesPlayed(Regions.SouthYorkshire),
      tracker.totalCoursesPlayed(Regions.WestYorkshire)
    ));
    chartData.put("region_course_count", List.of(
      tracker.totalCourseCount(Regions.NorthYorkshire),
      tracker.totalCourseCount(Regions.EastYorkshire),
      tracker.totalCourseCount(Regions.SouthYorkshire),
      tracker.totalCourseCount(Regions.WestYorkshire)
    ));

    YorkshireGolfPageTemplate pageTemplate = new YorkshireGolfPageTemplate()
      .withRequest(request)
      .withCurrentPageBasePath("/challenge")
      .withTitle("Yorkshire Challenge Tracker")
      .withPageScripts(
        chartJsLibScript(),
        chartJsPluginDataLabelsLibScript(),
        chartJsPluginDoughnutLabelLibScript()
      )
      .withBody(
        // Page header
        div().withClass("ygl-page-header").with(
          div().withClass("container").with(
            h1("#yorkshiregolfchallenge").withClass("ygl-page-header__title"),
            p("Track your progress through every golf course across the four ridings of Yorkshire.").withClass("ygl-page-header__lead")
          )
        ),
        // Chart
        div().withClass("container ygl-page").with(
          div().withClass("row justify-content-center").with(
            div().withClass("col-lg-8").with(
              div()
                .withClass("ygl-chart")
                .with(
                  canvas().withId("myChart")
                ),
              chartJsConfigScript("myChart", chartData),
              iff(canAddRound,
                div().withClass("mt-4").with(
                  errorMessage != null ? div(errorMessage).withClass("alert alert-danger") : text(""),
                  successMessage != null ? div(successMessage).withClass("alert alert-success") : text(""),
                  div().withClass("card mb-4").with(
                    div().withClass("card-header").with(strong("Add a Round")),
                    div().withClass("card-body").with(
                      form().withMethod("post").withAction("/challenge/" + trackerId + "/add-round").with(
                        csrfField,
                        div().withClass("row g-3 align-items-end").with(
                          div().withClass("col-md-6").with(
                            label("Course").withFor("courseName").withClass("form-label"),
                            select().withId("courseName").withName("courseName")
                              .withClass("form-select").attr("required", "").with(
                                option("— Select a course —").withValue("").attr("disabled", "").attr("selected", ""),
                                each(allCourses, course -> option(course.name()).withValue(course.name()))
                              )
                          ),
                          div().withClass("col-md-4").with(
                            label("Date Played").withFor("date").withClass("form-label"),
                            input().withType("date").withId("date").withName("date")
                              .withClass("form-control").attr("required", "")
                          ),
                          div().withClass("col-md-2").with(
                            button("Add").withType("submit").withClass("btn ygl-btn ygl-btn--primary w-100")
                          )
                        )
                      )
                    )
                  )
                )
              )
            )
          )
        )
      );

    pageTemplate.render(response.getWriter());

    log.info("View finished");

  }

}
