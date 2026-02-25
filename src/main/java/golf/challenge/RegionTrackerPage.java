package golf.challenge;

import golf.course.Regions;
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
  public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

    RegionChallengeTracker tracker = (RegionChallengeTracker) model.get("tracker");

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
              chartJsConfigScript("myChart", chartData)
            )
          )
        )
      );

    pageTemplate.render(response.getWriter());

    log.info("View finished");

  }

}
