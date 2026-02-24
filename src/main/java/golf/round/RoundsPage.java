package golf.round;

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
    public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

        @SuppressWarnings("unchecked")
        List<Round> rounds = (List<Round>) model.get("rounds");

        new YorkshireGolfPageTemplate()
                .withTitle("Yorkshire Golf Life - Rounds")
                .withBody(
                        div().withClass("container ygl-page").with(
                                h1("Rounds").withClass("ygl-page__title"),
                                p("Progress, rounds and reflections on the Yorkshire Golf Challenge.").withClass("ygl-page__lead"),
                                div().withClass("row row-cols-1 row-cols-md-2 g-4").with(
                                        rounds.stream().map(round ->
                                                div().withClass("col").with(
                                                        div().withClass("ygl-card h-100").with(
                                                                div().withClass("ygl-card__body").with(
                                                                        h5(round.title()).withClass("ygl-card__title"),
                                                                        p().withClass("ygl-card__meta mb-2").with(
                                                                                span(round.date()),
                                                                                round.courseName() != null && !round.courseName().isBlank()
                                                                                        ? span(" · " + round.courseName())
                                                                                        : span("")
                                                                        ),
                                                                        p(round.content().length() > 160
                                                                                ? round.content().substring(0, 160) + "…"
                                                                                : round.content()
                                                                        ).withClass("ygl-card__text"),
                                                                        a("Read more →")
                                                                                .withClass("ygl-btn ygl-btn--outline ygl-btn--sm mt-2")
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
