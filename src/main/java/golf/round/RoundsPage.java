package golf.round;

import golf.user.UserRound;
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
public class RoundsPage implements View {

    @Override
    public @Nullable String getContentType() {
        return MediaType.TEXT_HTML_VALUE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
                response.setContentType(MediaType.TEXT_HTML_VALUE);
                response.setCharacterEncoding("UTF-8");

        List<UserRound> rounds = (List<UserRound>) model.get("rounds");

        new YorkshireGolfPageTemplate().withRequest(request)
                .withTitle("Yorkshire Golf Life - Rounds")
                .withBody(
                        // Page header
                        div().withClass("ygl-page-header").with(
                                div().withClass("container").with(
                                        h1("Rounds").withClass("ygl-page-header__title"),
                                        p("Progress, rounds and reflections on the Yorkshire Golf Challenge.").withClass("ygl-page-header__lead")
                                )
                        ),
                        // Content
                        div().withClass("container ygl-page").with(
                                div().withClass("row row-cols-1 row-cols-md-2 g-4").with(
                                        rounds.stream().map(round ->
                                                div().withClass("col").with(
                                                        div().withClass("ygl-card h-100").with(
                                                                div().withClass("ygl-card__body").with(
                                                                        p().withClass("ygl-card__meta mb-2").with(
                                                                                span(round.date()),
                                                                                round.courseName() != null && !round.courseName().isBlank()
                                                                                        ? span(" · " + round.courseName())
                                                                                        : span("")
                                                                        ),
                                                                        h5(round.title() != null ? round.title() : round.courseName()).withClass("ygl-card__title"),
                                                                        round.content() != null
                                                                                ? p(round.content().length() > 160
                                                                                        ? round.content().substring(0, 160) + "…"
                                                                                        : round.content()
                                                                                  ).withClass("ygl-card__text")
                                                                                : text(""),
                                                                        a("Read more →")
                                                                                .withClass("ygl-btn ygl-btn--outline ygl-btn--sm mt-auto")
                                                                                .withHref("/rounds/" + round.id())
                                                                )
                                                        )
                                                )
                                        ).toArray(j2html.tags.DomContent[]::new)
                                )
                        )
                )
                .render(response.getWriter());

        log.info("RoundsPage rendered");
    }

}
