package golf.user;

import golf.web.YorkshireGolfPageTemplate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.View;

import java.util.List;
import java.util.Map;

import golf.utils.security.CsrfUtil;

import static j2html.TagCreator.*;

@Component
public class RegisterPage implements View {
    private final golf.security.FederatedProviders providers;
    private final LocalRegistration localRegistration;

    public RegisterPage(golf.security.FederatedProviders providers, LocalRegistration localRegistration) {
        this.providers = providers;
        this.localRegistration = localRegistration;
    }

    @Override
    public @Nullable String getContentType() {
        return MediaType.TEXT_HTML_VALUE;
    }

    @Override
    public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        boolean providerRequired = Boolean.TRUE.equals(model.get("providerRequired"));
        boolean localRegistrationEnabled = localRegistration.enabled()
                && Boolean.TRUE.equals(model.get("localRegistrationEnabled"));
        String errorMessage = (String) model.get("error");
        String emailValue = model.get("email") instanceof String email ? email : "";
        List<String> enabledProviders = providers.enabled();
        String enabledProviderNames = formatProviderNames(enabledProviders);
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.setCharacterEncoding("UTF-8");

        new YorkshireGolfPageTemplate().withRequest(request)
                .withTitle("Register — Yorkshire Golf Life")
                .withBody(
                        div().withClass("ygl-page-header").with(
                                div().withClass("container").with(
                                        h1("Create Account").withClass("ygl-page-header__title"),
                                        p("Join the Yorkshire Golf Challenge and track your rounds.").withClass("ygl-page-header__lead")
                                )
                        ),
                        div().withClass("container ygl-page").with(
                                div().withClass("row justify-content-center").with(
                                        div().withClass("col-md-8 col-lg-6").with(
                                                errorMessage != null
                                                        ? div(errorMessage).withClass("alert alert-danger").attr("role", "alert")
                                                        : text(""),
                                                providerRequired
                                                        ? div("New accounts must be created using one of these sign-in providers.")
                                                                .withClass("alert alert-info").attr("role", "status")
                                                        : text(""),
                                                localRegistrationEnabled
                                                        ? div("Local testing only: email and password registration is enabled. "
                                                                + "This option is restricted to localhost and is not available on the live site.")
                                                                .withClass("alert alert-warning").attr("role", "status")
                                                        : p("New accounts must be created using one of these sign-in providers to help keep your data safe. "
                                                                + "Choose an available provider below; your account will be created when you sign in.")
                                                                .withClass("ygl-register-provider-note"),
                                                enabledProviders.isEmpty()
                                                        && !localRegistrationEnabled
                                                        ? p("No sign-in providers are available right now. Please check back soon.")
                                                        : text(""),
                                                enabledProviders.isEmpty()
                                                        ? text("")
                                                        : div().withClass("ygl-login-panel ygl-login-panel--social").with(
                                                                h2("Create an account with a social provider").withClass("ygl-login-panel__title"),
                                                                div().with(each(enabledProviders, provider ->
                                                                        a().with(
                                                                                        img().withSrc("/images/auth/" + provider + ".svg").withAlt("")
                                                                                                .attr("aria-hidden", "true").attr("width", "20").attr("height", "20"),
                                                                                        span("Continue with " + golf.security.FederatedProviders.label(provider)))
                                                                                .withHref("/oauth2/authorization/" + provider)
                                                                                .attr("aria-label", "Continue with " + golf.security.FederatedProviders.label(provider))
                                                                                .attr("hx-boost", "false")
                                                                                .withClass("ygl-provider-sign-in ygl-provider-sign-in--" + provider))),
                                                                p("Already have an account? Sign in with your existing method first, then link "
                                                                        + enabledProviderNames + " from Sign-in methods to avoid creating a separate account.")
                                                                        .withClass("ygl-provider-sign-in__hint ygl-existing-account-hint")
                                                        ),
                                                localRegistrationEnabled
                                                        ? div().withClass("ygl-login-panel ygl-login-panel--email").with(
                                                                h2("Create an account with email").withClass("ygl-login-panel__title"),
                                                                form().withMethod("post").withAction("/register").with(
                                                                        CsrfUtil.csrfInput(request),
                                                                        div().withClass("mb-3").with(
                                                                                label("Email address").withFor("email").withClass("form-label"),
                                                                                input().withType("email").withId("email").withName("email")
                                                                                        .withValue(emailValue).withClass("form-control")
                                                                                        .attr("autocomplete", "email").attr("required", "")
                                                                        ),
                                                                        div().withClass("mb-3").with(
                                                                                label("Password").withFor("password").withClass("form-label"),
                                                                                input().withType("password").withId("password").withName("password")
                                                                                        .withClass("form-control").attr("required", "")
                                                                                        .attr("minlength", "8").attr("pattern", "[A-Za-z0-9]+")
                                                                                        .attr("title", "Letters and numbers only, at least 8 characters"),
                                                                                div("Must be at least 8 characters, using letters and numbers only.")
                                                                                        .withClass("form-text")
                                                                        ),
                                                                        div().withClass("mb-3").with(
                                                                                label("Confirm Password").withFor("confirmPassword").withClass("form-label"),
                                                                                input().withType("password").withId("confirmPassword")
                                                                                        .withName("confirmPassword").withClass("form-control")
                                                                                        .attr("autocomplete", "new-password").attr("required", "")
                                                                        ),
                                                                        button("Create Account").withType("submit")
                                                                                .withClass("btn ygl-btn ygl-btn--primary w-100")
                                                                )
                                                        )
                                                        : text(""),
                                                div().withClass("ygl-login-footer text-center").with(
                                                        p().with(
                                                                text("Already have an account? "),
                                                                a("Sign in").withHref("/login")
                                                        )
                                                )
                                        )
                                )
                        )
                )
                .render(response.getWriter());
    }

    private static String formatProviderNames(List<String> providers) {
        List<String> names = providers.stream().map(golf.security.FederatedProviders::label).toList();
        if (names.size() < 2) return String.join("", names);
        if (names.size() == 2) return names.get(0) + " and " + names.get(1);
        return String.join(", ", names.subList(0, names.size() - 1)) + ", or " + names.get(names.size() - 1);
    }

}
