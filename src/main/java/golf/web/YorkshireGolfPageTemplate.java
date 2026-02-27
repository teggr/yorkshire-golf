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
                                a().withClass("navbar-brand ygl-navbar__brand").withHref("/").with(
                                        text("Yorkshire Golf Life")
                                ),
                                button()
                                        .withClass("navbar-toggler")
                                        .attr("type", "button")
                                        .attr("data-bs-toggle", "collapse")
                                        .attr("data-bs-target", "#navbarNav")
                                        .with(span().withClass("navbar-toggler-icon")),
                                div().withClass("collapse navbar-collapse justify-content-end").withId("navbarNav").with(
                                        ul().withClass("navbar-nav ygl-navbar__nav").with(
                                                li().withClass("nav-item").with(
                                                        a("Challenge").withClass("nav-link ygl-navbar__link").withHref("/challenge")
                                                ),
                                                li().withClass("nav-item").with(
                                                        a("Rounds").withClass("nav-link ygl-navbar__link").withHref("/rounds")
                                                ),
                                                li().withClass("nav-item").with(
                                                        a("Courses").withClass("nav-link ygl-navbar__link").withHref("/courses")
                                                )
                                        )
                                )
                        )
                );
    }

    private DomContent buildFooter() {
        return footer()
                .withClass("ygl-footer")
                .with(
                        div().withClass("ygl-footer__top").with(
                                div().withClass("container").with(
                                        div().withClass("row g-4").with(
                                                div().withClass("col-lg-5").with(
                                                        div().withClass("ygl-footer__brand").with(text("Yorkshire Golf Life")),
                                                        p("Tracking the journey to play every golf course across the four ridings of Yorkshire.").withClass("ygl-footer__tagline")
                                                ),
                                                div().withClass("col-6 col-lg-2 offset-lg-2").with(
                                                        div().withClass("ygl-footer__heading").with(text("Explore")),
                                                        ul().withClass("ygl-footer__link-list").with(
                                                                li().with(a("Home").withHref("/")),
                                                                li().with(a("Courses").withHref("/courses")),
                                                                li().with(a("Rounds").withHref("/rounds"))
                                                        )
                                                ),
                                                div().withClass("col-6 col-lg-2").with(
                                                        div().withClass("ygl-footer__heading").with(text("Challenge")),
                                                        ul().withClass("ygl-footer__link-list").with(
                                                                li().with(a("Tracker").withHref("/challenge")),
                                                                li().with(a("#yorkshiregolfchallenge").withHref("/challenge"))
                                                        )
                                                )
                                        )
                                )
                        ),
                        div().withClass("ygl-footer__bottom").with(
                                div().withClass("container").with(
                                        text("© Yorkshire Golf Life")
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
                                        googleFontsPreconnect(),
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
