package golf.web;

import golf.utils.security.CsrfUtil;
import j2html.rendering.IndentedHtml;
import j2html.tags.DomContent;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static golf.utils.bootstrap.BootstrapTagCreator.*;
import static golf.utils.htmx.HtmxAttributes.hxBoost;
import static j2html.TagCreator.*;

public class YorkshireGolfPageTemplate {

    private String title;
    private DomContent[] pageScripts = new DomContent[0];
    private DomContent[] body = new DomContent[0];
    private HttpServletRequest request;
        private String currentPageBasePath;

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

    public YorkshireGolfPageTemplate withRequest(HttpServletRequest request) {
        this.request = request;
        return this;
    }

        public YorkshireGolfPageTemplate withCurrentPageBasePath(String currentPageBasePath) {
                this.currentPageBasePath = currentPageBasePath;
                return this;
        }

        private boolean requestPathMatchesCurrentPage() {
                if (request == null || currentPageBasePath == null || currentPageBasePath.isBlank()) {
                        return false;
                }

                String requestPath = request.getRequestURI();
                if (requestPath == null || requestPath.isBlank()) {
                        return false;
                }

                if ("/".equals(currentPageBasePath)) {
                        return "/".equals(requestPath);
                }

                return requestPath.equals(currentPageBasePath) || requestPath.startsWith(currentPageBasePath + "/");
        }

        private String navbarLinkClass(String linkPath) {
                String baseClass = "nav-link ygl-navbar__link";
                if (requestPathMatchesCurrentPage() && linkPath.equals(currentPageBasePath)) {
                        return baseClass + " ygl-navbar__link--active";
                }
                return baseClass;
        }

        private DomContent navbarLink(String label, String linkPath) {
                var link = a(label).withClass(navbarLinkClass(linkPath)).withHref(linkPath);
                if (requestPathMatchesCurrentPage() && linkPath.equals(currentPageBasePath)) {
                        link.attr("aria-current", "page");
                }
                return link;
        }

    private static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
    }

    private DomContent buildNavbar() {
        boolean loggedIn = isAuthenticated();
        DomContent csrfField = request != null ? CsrfUtil.csrfInput(request) : text("");
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
                                                        navbarLink("Courses", "/courses")
                                                ),
                                                li().withClass("nav-item").with(
                                                        navbarLink("Top 100", "/top-100")
                                                ),
                                                li().withClass("nav-item").with(
                                                        navbarLink("Next 100", "/next-100")
                                                ),
                                                li().withClass("nav-item").with(
                                                        navbarLink("Play & Stay", "/play-and-stay")
                                                ),
                                                li().withClass("nav-item").with(
                                                        navbarLink("Yorkshire Challenge", "/challenge")
                                                ),
                                                iffElse(!loggedIn,
                                                        li().withClass("nav-item").with(
                                                                a("Sign In").withClass("nav-link ygl-navbar__link").withHref("/login")
                                                        ),
                                                        text("")
                                                ),
                                                iffElse( loggedIn,
                                                         li().withClass("nav-item").with(
                                                                form().withMethod("post").withAction("/logout")
                                                                        .withClass("d-inline").with(
                                                                        csrfField,
                                                                        button("Sign Out").withType("submit")
                                                                                .withClass("nav-link ygl-navbar__link btn btn-link")
                                                                )
                                                          ),
                                                           text("")
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
                                                div().withClass("col-lg-4").with(
                                                        div().withClass("ygl-footer__brand").with(text("Yorkshire Golf Life")),
                                                        p("Celebrating Yorkshire's exceptional golfing heritage").withClass("ygl-footer__tagline")
                                                ),
                                                div().withClass("col-6 col-lg-2").with(
                                                        div().withClass("ygl-footer__heading").with(text("Explore")),
                                                        ul().withClass("ygl-footer__link-list").with(
                                                                li().with(a("Home").withHref("/")),
                                                                li().with(a("Courses").withHref("/courses")),
                                                                li().with(a("Top 100").withHref("/top-100")),
                                                                li().with(a("Next 100").withHref("/next-100")),
                                                                li().with(a("Play & Stay").withHref("/play-and-stay"))
                                                                
                                                        )
                                                ),
                                                div().withClass("col-6 col-lg-3").with(
                                                        div().withClass("ygl-footer__heading").with(text("Challenge")),
                                                        ul().withClass("ygl-footer__link-list").with(
                                                                li().with(a("#yorkshiregolfchallenge").withHref("/challenge"))
                                                        )
                                                ),
                                                div().withClass("col-6 col-lg-3").with(
                                                        div().withClass("ygl-footer__heading").with(text("Socials")),
                                                        ul().withClass("ygl-footer__link-list").with(
                                                                li().with(
                                                                        a().withHref("https://www.instagram.com/yorkshiregolflife/")
                                                                                .withTarget("_blank")
                                                                                .withRel("noopener noreferrer")
                                                                                .with(
                                                                                        i().withClass("bi bi-instagram me-2"),
                                                                                        text("@yorkshiregolflife")
                                                                                )
                                                                )
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
                                        bootstrapIconsCssLinkTag(),
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
