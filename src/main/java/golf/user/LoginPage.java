package golf.user;

import golf.web.YorkshireGolfPageTemplate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.View;

import java.util.Map;

import golf.utils.security.CsrfUtil;

import static j2html.TagCreator.*;

@Component
public class LoginPage implements View {
    private final golf.security.FederatedProviders providers;

    public LoginPage(golf.security.FederatedProviders providers) {
        this.providers = providers;
    }

    @Override
    public @Nullable String getContentType() {
        return MediaType.TEXT_HTML_VALUE;
    }

    @Override
    public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
                response.setContentType(MediaType.TEXT_HTML_VALUE);
                response.setCharacterEncoding("UTF-8");

        boolean hasError = Boolean.TRUE.equals(model.get("error"));
        boolean existing = Boolean.TRUE.equals(model.get("existing"));
        String existingProvider = model.get("existingProvider") instanceof String provider
                ? provider : "your provider";
        boolean registered = Boolean.TRUE.equals(model.get("registered"));
        boolean deleted = Boolean.TRUE.equals(model.get("deleted"));

        new YorkshireGolfPageTemplate().withRequest(request)
                .withTitle("Login — Yorkshire Golf Life")
                .withBody(
                        div().withClass("ygl-page-header").with(
                                div().withClass("container").with(
                                        h1("Sign In").withClass("ygl-page-header__title"),
                                        p("Log in to manage your rounds and track your Yorkshire Golf Challenge.").withClass("ygl-page-header__lead")
                                )
                        ),
                        div().withClass("container ygl-page").with(
                                div().withClass("row justify-content-center").with(
                                        div().withClass("col-md-6 col-lg-4").with(
                                                existing
                                                        ? div("This " + existingProvider + " account uses an email already registered here. Sign in with your existing method first, then go to Sign-in methods and link " + existingProvider + ". No new account was created.")
                                                                .withClass("alert alert-info").attr("role", "status")
                                                        : hasError
                                                        ? div("Unable to sign in. Please try again or use another sign-in method.").withClass("alert alert-danger")
                                                        : text(""),
                                                registered
                                                        ? div("Account created successfully! Please sign in.").withClass("alert alert-success")
                                                        : text(""),
                                                deleted
                                                        ? div("Your account and golf rounds have been deleted.").withClass("alert alert-success").attr("role", "status")
                                                        : text(""),
                                                div().withClass("alert alert-info ygl-auth-transition").attr("role", "status").with(
                                                        text("A note from "),
                                                        a("@yorkshiregolflife on Instagram")
                                                                .withHref("https://www.instagram.com/yorkshiregolflife/")
                                                                .attr("target", "_blank").attr("rel", "noopener noreferrer"),
                                                        text(": we're moving sign-in to trusted providers to help keep your data safe. "
                                                                + "For now, existing members can still sign in with email and password, then visit "
                                                                + "Sign-in methods in their account to link Facebook, X, Apple, or Google. "
                                                                + "New accounts must use one of those providers.")
                                                ),
                                                div().with(each(providers.enabled(), provider ->
                                                        a().with(
                                                                       img().withSrc("/images/auth/" + provider + ".svg").withAlt("")
                                                                               .attr("aria-hidden", "true").attr("width", "20").attr("height", "20"),
                                                                       span("Continue with " + golf.security.FederatedProviders.label(provider)))
                                                                .withHref("/oauth2/authorization/" + provider)
                                                                .attr("aria-label", "Continue with " + golf.security.FederatedProviders.label(provider))
                                                                .attr("hx-boost", "false")
                                                                .withClass("ygl-provider-sign-in ygl-provider-sign-in--" + provider))),
                                                form().withMethod("post").withAction("/login").with(
                                                        CsrfUtil.csrfInput(request),
                                                        div().withClass("mb-3").with(
                                                                label("Email address").withFor("email").withClass("form-label"),
                                                                input().withType("email").withId("email").withName("username")
                                                                        .withClass("form-control").attr("required", "")
                                                        ),
                                                        div().withClass("mb-3").with(
                                                                label("Password").withFor("password").withClass("form-label"),
                                                                input().withType("password").withId("password").withName("password")
                                                                        .withClass("form-control").attr("required", "")
                                                        ),
                                                        button("Sign In").withType("submit").withClass("btn ygl-btn ygl-btn--primary w-100 mb-3")
                                                ),
                                                div().withClass("text-center").with(
                                                        p().with(
                                                                text("Don't have an account? "),
                                                                a("Register here").withHref("/register")
                                                        ),
                                                        p().with(
                                                                a("Forgot your password?").withHref("/forgot-password")
                                                        )
                                                )
                                        )
                                )
                        )
                )
                .render(response.getWriter());
    }

}
