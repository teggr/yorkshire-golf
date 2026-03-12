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

import static j2html.TagCreator.*;

@Component
@Slf4j
public class PlayAndStayPage implements View {

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

        new YorkshireGolfPageTemplate().withRequest(request)
                .withCurrentPageBasePath("/play-and-stay")
                .withTitle("Play & Stay – Yorkshire Golf")
                .withDescription("Find Yorkshire golf clubs offering play and stay packages with onsite accommodation. Stay longer, play more, and experience the best of Yorkshire golf.")
                .withBody(
                        // Page header
                        div().withClass("ygl-page-header").with(
                                div().withClass("container").with(
                                        h1("Play & Stay").withClass("ygl-page-header__title"),
                                        p("Discover Yorkshire golf courses with onsite accommodation – stay longer and play more.").withClass("ygl-page-header__lead")
                                )
                        ),
                        // Course listings
                        courseGrid(courses)
                )
                .render(response.getWriter());

        log.info("PlayAndStayPage rendered");
    }

    static j2html.tags.DomContent courseGrid(List<Course> courses) {
        return div().withClass("container ygl-page").with(
                courses.isEmpty()
                        ? p("No Play & Stay courses found.").withClass("text-muted")
                        : div().withClass("row row-cols-1 row-cols-md-2 row-cols-lg-3 g-3").with(
                                courses.stream()
                                        .map(course -> {
                                            String playAndStayImage = course.stayImageUrl() != null && !course.stayImageUrl().isEmpty()
                                                    ? course.stayImageUrl()
                                                    : course.mainImageUrl();
                                            return div().withClass("col").with(CoursesPage.courseCard(course, playAndStayImage));
                                        })
                                        .toArray(j2html.tags.DomContent[]::new)
                        )
        );
    }

}
