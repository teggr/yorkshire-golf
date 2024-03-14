package golf.web;

import golf.tracker.CourseTracker;
import golf.tracker.Regions;
import golf.utils.j2html.J2HtmlTemplateView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static golf.utils.chartjs.ChartJsCreator.*;
import static j2html.TagCreator.*;

@Component
@Slf4j
class HomePage extends J2HtmlTemplateView {

    @Override
    protected void renderMergedTemplateModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

        log.info("View called");

        CourseTracker tracker = (CourseTracker) model.get("tracker");

        Map<String, Object> chartData = new HashMap<>();
        chartData.put("overall_percentage",
                tracker.totalCoursesPlayed() * 100 / tracker.totalCourseCount()
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
                        h1("#yorkshiregolfchallenge"),
                        div()
                                .withStyle("max-width: 640px")
                                .with(
                                        canvas().withId("myChart")
                                ),
                        chartJsConfigScript("myChart", chartData)
                );

        pageTemplate.render(response.getWriter());

        log.info("View finished");

    }

}
