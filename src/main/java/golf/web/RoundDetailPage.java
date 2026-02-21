package golf.web;

import golf.round.Round;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.View;

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

        new YorkshireGolfPageTemplate()
                .withTitle(round.title() + " - Yorkshire Golf Life")
                .withBody(
                        div().withClass("container py-4").with(
                                a("← Back to rounds").withHref("/rounds").withClass("text-decoration-none text-muted mb-4 d-inline-block"),
                                div().withClass("row justify-content-center").with(
                                        div().withClass("col-lg-8").with(
                                                article().with(
                                                        h1(round.title()).withClass("display-6 mb-1"),
                                                        p().withClass("text-muted mb-3").with(
                                                                span(round.date()),
                                                                round.courseName() != null && !round.courseName().isBlank()
                                                                        ? span().with(
                                                                                text(" · "),
                                                                                span(round.courseName()).withClass("badge bg-dark fw-normal")
                                                                          )
                                                                        : span("")
                                                        ),
                                                        imagesSection(round.imageUrls(), round.title()),
                                                        div().withClass("fs-5 lh-lg").with(
                                                                p(round.content())
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
                    .withClass("img-fluid rounded mb-4 w-100");
        }
        return div().withClass("row g-2 mb-4").with(
                imageUrls.stream()
                        .map(url -> div().withClass("col-6").with(
                                img().withSrc(url)
                                        .withAlt(alt)
                                        .withClass("img-fluid rounded w-100 h-100 object-fit-cover")
                        ))
                        .toArray(j2html.tags.DomContent[]::new)
        );
    }

}
