package golf.challenge;

import golf.web.YorkshireGolfPageTemplate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.View;

import java.util.Map;

import static j2html.TagCreator.*;

@Component
public class ChallengeLandingPage implements View {

    @Override
    public @Nullable String getContentType() {
        return MediaType.TEXT_HTML_VALUE;
    }

    @Override
    public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.setCharacterEncoding("UTF-8");

        int totalCourseCount = 0;
        if (model != null && model.get("totalCourseCount") instanceof Number total) {
            totalCourseCount = total.intValue();
        }

        new YorkshireGolfPageTemplate()
                .withRequest(request)
                .withCurrentPageBasePath("/challenge")
                .withTitle("Challenge Tracker — Yorkshire Golf Life")
                .withBody(
                        div().withClass("ygl-page-header").with(
                                div().withClass("container").with(
                                        h1("Challenge Tracker").withClass("ygl-page-header__title"),
                                        p("A challenge to play all " + totalCourseCount + " courses across Yorkshire, spread across the four regions.")
                                                .withClass("ygl-page-header__lead")
                                )
                        ),
                        div().withClass("container ygl-page").with(
                                div().withClass("row g-4 align-items-center").with(
                                        div().withClass("col-12 col-lg-6").with(
                                                h2("Track your Yorkshire Golf Challenge progress").withClass("h3 mb-3"),
                                                p("By signing up to Yorkshire Golf Life, you'll get your own tracker where you can register your rounds and see your challenge info chart.")
                                                        .withClass("mb-3"),
                                                div().withClass("d-flex flex-wrap gap-2").with(
                                                        a("Create an account").withHref("/register").withClass("ygl-btn ygl-btn--primary"),
                                                        a("Sign in").withHref("/login").withClass("ygl-btn ygl-btn--outline")
                                                )
                                        ),
                                        div().withClass("col-12 col-lg-6").with(
                                                div().withClass("ygl-chart").with(
                                                        img().withSrc("/images/challenge/challenge-tracker-landing.svg")
                                                                .withAlt("Example challenge info chart showing all Yorkshire courses played")
                                                                .withClass("img-fluid")
                                                )
                                        )
                                )
                        )
                )
                .render(response.getWriter());
    }
}