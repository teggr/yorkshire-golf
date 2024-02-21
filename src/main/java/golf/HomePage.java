package golf;

import golf.htmx.HtmxAttributes;
import j2html.rendering.IndentedHtml;
import j2html.tags.DomContent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.view.AbstractTemplateView;

import java.util.Map;

import static j2html.TagCreator.*;

@Component
@Slf4j
class HomePage extends AbstractTemplateView {

    @Override
    protected boolean isUrlRequired() {
        return false;
    }

    @Override
    protected void renderMergedTemplateModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

        log.info("View called");

        DomContent html = html()
                .attr("lang", "en")
                .with(
                        head(
                                meta()
                                        .attr("charset", "utf-8"),
                                meta()
                                        .withName("viewport")
                                        .attr("content", "width=device-width, initial-scale=1"),
                                title("Bootstrap demo"),
                                link()
                                        .withHref("https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css")
                                        .attr("rel", "stylesheet")
                                        .attr("integrity", "sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH")
                                        .attr("crossorigin", "anonymous")
                        ),
                        body()
                                .attr(HtmxAttributes.hxBoost())
                                .with(
                                h1("Hello, world!"),
                                script()
                                        .attr("src", "https://cdn.jsdelivr.net/npm/@popperjs/core@2.11.8/dist/umd/popper.min.js")
                                        .attr("integrity", "sha384-I7E8VVD/ismYTF4hNIPjVp/Zjvgyol6VFvRkX/vR+Vc4jQkC+hVqc2pM8ODewa9r")
                                        .attr("crossorigin", "anonymous"),
                                script()
                                        .attr("src", "https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.min.js")
                                        .attr("integrity", "sha384-0pUGZvbkm6XF6gxjEnlmuGrJXVbNuzT9qBBavbLwCsOGabYfZo0T0to5eqruptLy")
                                        .attr("crossorigin", "anonymous")
                        )
                );

        // write the document to the output stream
        join(document(), html).render(IndentedHtml.into(response.getWriter()));

        log.info("View finished");

    }

}
