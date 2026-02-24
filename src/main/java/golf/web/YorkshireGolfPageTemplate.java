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
                .withClass("navbar navbar-expand-lg ygl-navbar")
                .attr("data-bs-theme", "dark")
                .with(
                        div().withClass("container").with(
                                a("⛳ Yorkshire Golf Life")
                                        .withClass("navbar-brand fw-bold ygl-navbar__brand")
                                        .withHref("/"),
                                div().withClass("navbar-nav flex-row gap-3").with(
                                        a("Challenge").withClass("nav-link ygl-navbar__link").withHref("/challenge"),
                                        a("Rounds").withClass("nav-link ygl-navbar__link").withHref("/rounds"),
                                        a("Courses").withClass("nav-link ygl-navbar__link").withHref("/courses")
                                )
                        )
                );
    }

    private DomContent buildFooter() {
        return footer()
                .withClass("ygl-footer")
                .with(
                        div().withClass("container").with(
                                p("© Yorkshire Golf Life").withClass("mb-0")
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
                                        googleFontsLinkTag(),
                                        bootstrapMinCssLinkTag(),
                                        themeCssLinkTag()
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
                                .with(buildFooter())
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
