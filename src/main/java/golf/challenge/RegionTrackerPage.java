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
    List<String> monthlyProgressLabels = model.get("monthlyProgressLabels") != null ? (List<String>) model.get("monthlyProgressLabels") : List.of();
    List<Long> monthlyProgressValues = model.get("monthlyProgressValues") != null ? (List<Long>) model.get("monthlyProgressValues") : List.of();
    long monthlyProgressMax = model.get("monthlyProgressMax") != null ? ((Number) model.get("monthlyProgressMax")).longValue() : tracker.totalCourseCount();
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

    Map<String, Object> monthlyProgressChartData = new HashMap<>();
    monthlyProgressChartData.put("timeline_labels", monthlyProgressLabels);
    monthlyProgressChartData.put("timeline_values", monthlyProgressValues);
    monthlyProgressChartData.put("timeline_max", monthlyProgressMax);

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
        // Sharing banner
        div().withClass("container ygl-page").with(
          div().withClass("row mt-4").with(
            div().withClass("col-12").with(
              div().withClass("alert ygl-share-banner mb-0").with(
                p("This page is public — feel free to share it! Send the link to friends, family, or fellow golfers, or screenshot your progress and show it off on the socials.").withClass("mb-0")
              )
            )
          )
        ),
        // Chart
        div().withClass("container ygl-page").with(
          div().withClass("row g-4 ygl-infographics-row").with(
            div().withClass("col-12 col-xl-6").with(
              div()
                .withClass("ygl-chart ygl-chart--full")
                .with(
                  canvas().withId("myChart")
                ),
              chartJsConfigScript("myChart", chartData)
            ),
            div().withClass("col-12 col-xl-6").with(
              div()
                .withClass("ygl-chart ygl-chart--full")
                .with(
                  canvas().withId("progressLineChart")
                ),
              chartJsConfigScript("progressLineChart", monthlyProgressChartData, "/progress-line-chart.js")
            )
          ),
          iff(canAddRound,
            div().withClass("row mt-4").with(
              div().withClass("col-12").with(
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
                ),
                div().withClass("card mb-4").with(
                  div().withClass("card-header").with(strong("Import Rounds from CSV")),
                  div().withClass("card-body").with(
                    p("Upload a CSV file to bulk-import your rounds. Your file should have a header row followed by one round per line, with two columns: course name and date played in YYYY-MM-DD format — for example: ").withClass("text-muted small mb-3").with(
                      code("courseName,date")
                    ),
                    form().withMethod("post").withAction("/challenge/" + trackerId + "/import-rounds")
                      .attr("enctype", "multipart/form-data").with(
                        csrfField,
                        div().withClass("row g-3 align-items-end").with(
                          div().withClass("col-md-10").with(
                            label("CSV File").withFor("csvFile").withClass("form-label"),
                            input().withType("file").withId("csvFile").withName("csvFile")
                              .withClass("form-control").attr("accept", ".csv").attr("required", "")
                          ),
                          div().withClass("col-md-2").with(
                            button("Import").withType("submit").withClass("btn ygl-btn ygl-btn--primary w-100")
                          )
                        )
                      )
                  )
                )
              )
            )
          ),
          iff(canAddRound,
            div().withClass("row").with(
              div().withClass("col-12").with(
                div().withClass("card").with(
                  div().withClass("card-header").with(strong("Your Played Courses")),
                  div().withClass("card-body").with(
                    tracker.rounds().isEmpty()
                      ? p("No rounds logged yet. Add a round above or import a CSV.").withClass("text-muted mb-0")
                      : div().withClass("table-responsive").with(
                        table().withClass("table align-middle mb-0").with(
                          thead().with(
                            tr().with(
                              th("Course"),
                              th("Region"),
                              th("Date"),
                              th("Actions")
                            )
                          ),
                          tbody().with(
                            each(tracker.rounds(), round -> tr().with(
                              td(round.courseName()),
                              td(round.course() != null ? round.course().region().displayName() : "-"),
                              td(round.date()),
                              td().withClass("ygl-round-actions").with(
                                form().withMethod("post").withAction("/challenge/" + trackerId + "/update-round-date")
                                  .withClass("ygl-round-actions__form").with(
                                    CsrfUtil.csrfInput(request),
                                    input().withType("hidden").withName("roundId").withValue(round.id()),
                                    input().withType("date").withName("date").withValue(round.date())
                                      .withClass("form-control form-control-sm"),
                                    button("Save date").withType("submit").withClass("btn ygl-btn ygl-btn--outline ygl-btn--sm")
                                  ),
                                form().withMethod("post").withAction("/challenge/" + trackerId + "/delete-round")
                                  .withClass("ygl-round-actions__delete")
                                  .attr("onsubmit", "return confirm('Are you sure you want to delete this round?');").with(
                                    CsrfUtil.csrfInput(request),
                                    input().withType("hidden").withName("roundId").withValue(round.id()),
                                    button("Delete").withType("submit").withClass("btn ygl-btn ygl-btn--dark ygl-btn--sm")
                                  )
                              )
                            ))
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
