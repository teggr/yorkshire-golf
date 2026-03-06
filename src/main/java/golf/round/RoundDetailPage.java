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
public class RoundDetailPage implements View {

    @Override
    public @Nullable String getContentType() {
        return MediaType.TEXT_HTML_VALUE;
    }

    @Override
    public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

        Round round = (Round) model.get("round");

        new YorkshireGolfPageTemplate().withRequest(request)
                .withTitle(round.title() + " - Yorkshire Golf Life")
                .withBody(
                        div().withClass("container ygl-page").with(
                                div().withClass("ygl-page__content").with(
                                        a("← Back to Rounds").withHref("/rounds").withClass("ygl-back-link"),
                                        div().withClass("row justify-content-center").with(
                                                div().withClass("col-lg-8").with(
                                                        article().withClass("ygl-article").with(
                                                                h1(round.title()).withClass("ygl-article__title"),
                                                                p().withClass("ygl-article__meta").with(
                                                                        span(round.date()),
                                                                        round.courseName() != null && !round.courseName().isBlank()
                                                                                ? span().with(
                                                                                        text(" · "),
                                                                                        span(round.courseName()).withClass("ygl-badge")
                                                                                  )
                                                                                : span("")
                                                                ),
                                                                imagesSection(round.imageUrls(), round.title()),
                                                                div().withClass("ygl-article__content").with(
                                                                        p(round.content())
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
                .render(response.getWriter());

        log.info("RoundDetailPage rendered for round: {}", round.id());
    }

    private j2html.tags.DomContent imagesSection(List<String> imageUrls, String alt) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return div();
        }
        if (imageUrls.size() == 1) {
            return img().withSrc(imageUrls.get(0))
                    .withAlt(alt)
                    .withClass("img-fluid rounded-theme mb-4 w-100 ygl-article__image");
        }
        return div().withClass("row g-2 mb-4").with(
                imageUrls.stream()
                        .map(url -> div().withClass("col-6").with(
                                img().withSrc(url)
                                        .withAlt(alt)
                                        .withClass("img-fluid rounded-theme w-100 h-100 object-fit-cover ygl-article__image")
                        ))
                        .toArray(j2html.tags.DomContent[]::new)
        );
    }

}
