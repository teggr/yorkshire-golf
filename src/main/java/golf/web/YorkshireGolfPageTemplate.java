package golf.web;

import j2html.rendering.IndentedHtml;
import j2html.tags.DomContent;

import java.io.IOException;

import static golf.utils.bootstrap.BootstrapTagCreator.*;
import static golf.utils.htmx.HtmxAttributes.hxBoost;
import static j2html.TagCreator.*;

public class YorkshireGolfPageTemplate {

    private String title;
    private DomContent[] pageScripts = new DomContent[0];
    private DomContent[] body = new DomContent[0];

    public YorkshireGolfPageTemplate withTitle(String title) {
        this.title = title;
        return this;
    }

    public YorkshireGolfPageTemplate withPageScripts(DomContent... pageScripts) {
        this.pageScripts = pageScripts;
        return this;
    }

    public YorkshireGolfPageTemplate withBody(DomContent... body) {
        this.body = body;
        return this;
    }

    private DomContent buildNavbar() {
        return nav()
                .withClass("navbar navbar-expand-lg bg-dark")
                .attr("data-bs-theme", "dark")
                .with(
                        div().withClass("container").with(
                                a("⛳ Yorkshire Golf Life")
                                        .withClass("navbar-brand fw-bold")
                                        .withHref("/"),
                                div().withClass("navbar-nav flex-row gap-3").with(
                                        a("Tracker").withClass("nav-link").withHref("/"),
                                        a("Courses").withClass("nav-link").withHref("/courses"),
                                        a("Rounds").withClass("nav-link").withHref("/rounds")
                                )
                        )
                );
    }

    private DomContent buildHtml() {
        return html()
                .attr("lang", "en")
                .with(
                        head()
                                .with(
                                        bootstrapCharsetMetaTag(),
                                        bootstrapViewportMetaTag(),
                                        title(title),
                                        bootstrapMinCssLinkTag()
                                )
                                .with(
                                        pageScripts
                                ),
                        body()
                                .attr(hxBoost())
                                .with(buildNavbar())
                                .with(
                                        body
                                )
                                .with(
                                        popperMinJsScriptTag(),
                                        bootstrapMinJsScriptTag()
                                )
                          .with(rawHtml("""
                            <!-- 100% privacy-first analytics -->
                            <script async src="https://scripts.simpleanalyticscdn.com/latest.js"></script>
                            """))
                );
    }

    public void render(Appendable out) throws IOException {

        // write the document to the output stream
        join(document(), buildHtml())
                .render(IndentedHtml.into(out));

    }

}
