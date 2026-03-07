package golf.user;

import golf.course.Course;
import golf.course.Courses;
import golf.utils.security.CsrfUtil;
import golf.web.YorkshireGolfPageTemplate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.View;

import java.util.List;
import java.util.Map;

import static j2html.TagCreator.*;

@Component
@RequiredArgsConstructor
public class MyRoundsPage implements View {

    private final Courses courses;

    @Override
    public @Nullable String getContentType() {
        return MediaType.TEXT_HTML_VALUE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
                response.setContentType(MediaType.TEXT_HTML_VALUE);
                response.setCharacterEncoding("UTF-8");

        List<UserRound> userRounds = model.get("userRounds") != null ? (List<UserRound>) model.get("userRounds") : List.of();
        String trackerId = (String) model.get("trackerId");
        String errorMessage = (String) model.get("error");
        String successMessage = (String) model.get("success");
        j2html.tags.DomContent csrfField = CsrfUtil.csrfInput(request);

        List<Course> allCourses = courses.getAllCourses();

        new YorkshireGolfPageTemplate().withRequest(request)
                .withTitle("My Rounds — Yorkshire Golf Life")
                .withBody(
                        div().withClass("ygl-page-header").with(
                                div().withClass("container").with(
                                        h1("My Rounds").withClass("ygl-page-header__title"),
                                        p("Track the Yorkshire courses you've played.").withClass("ygl-page-header__lead"),
                                        trackerId != null
                                                ? p().withClass("ygl-page-header__lead").with(
                                                        text("Your public tracker: "),
                                                        a("/challenge/" + trackerId)
                                                                .withHref("/challenge/" + trackerId)
                                                                .withClass("text-white")
                                                )
                                                : text("")
                                )
                        ),
                        div().withClass("container ygl-page").with(
                                errorMessage != null ? div(errorMessage).withClass("alert alert-danger") : text(""),
                                successMessage != null ? div(successMessage).withClass("alert alert-success") : text(""),

                                // Add round form
                                div().withClass("card mb-4").with(
                                        div().withClass("card-header").with(strong("Add a Round")),
                                        div().withClass("card-body").with(
                                                form().withMethod("post").withAction("/my-rounds/add").with(
                                                        csrfField,
                                                        div().withClass("row g-3 align-items-end").with(
                                                                div().withClass("col-md-6").with(
                                                                        label("Course").withFor("courseName").withClass("form-label"),
                                                                        select().withId("courseName").withName("courseName")
                                                                                .withClass("form-select").attr("required", "").with(
                                                                                        option("— Select a course —").withValue("").attr("disabled", "").attr("selected", ""),
                                                                                        each(allCourses, course ->
                                                                                                option(course.name()).withValue(course.name())
                                                                                        )
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

                                // Rounds list
                                userRounds.isEmpty()
                                        ? div().withClass("text-center text-muted py-5").with(
                                                p("You haven't logged any rounds yet. Add your first round above!")
                                          )
                                        : div().withClass("table-responsive").with(
                                                table().withClass("table table-hover align-middle").with(
                                                        thead().with(
                                                                tr().with(
                                                                        th("Course"),
                                                                        th("Date Played"),
                                                                        th("Actions")
                                                                )
                                                        ),
                                                        tbody().with(
                                                                each(userRounds, round ->
                                                                        tr().with(
                                                                                td(round.courseName()),
                                                                                td().with(
                                                                                        form().withMethod("post").withAction("/my-rounds/edit/" + round.id()).with(
                                                                                                csrfField,
                                                                                                input().withType("date").withName("date")
                                                                                                        .withValue(round.date())
                                                                                                        .withClass("form-control form-control-sm d-inline-block")
                                                                                                        .attr("style", "width: auto;")
                                                                                                        .attr("required", ""),
                                                                                                button("Save").withType("submit")
                                                                                                        .withClass("btn btn-sm btn-outline-secondary ms-2")
                                                                                        )
                                                                                ),
                                                                                td().with(
                                                                                        form().withMethod("post").withAction("/my-rounds/delete/" + round.id()).with(
                                                                                                csrfField,
                                                                                                button("Delete").withType("submit")
                                                                                                        .withClass("btn btn-sm btn-outline-danger")
                                                                                                        .attr("onclick", "return confirm('Delete this round?')")
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                          )
                        )
                )
                .render(response.getWriter());
    }

}
