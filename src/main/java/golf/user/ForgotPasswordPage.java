package golf.user;

import golf.utils.security.CsrfUtil;
import golf.web.YorkshireGolfPageTemplate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.View;

import java.util.Map;

import static j2html.TagCreator.*;

@Component
public class ForgotPasswordPage implements View {

    @Override
    public @Nullable String getContentType() {
        return MediaType.TEXT_HTML_VALUE;
    }

    @Override
    public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
                response.setContentType(MediaType.TEXT_HTML_VALUE);
                response.setCharacterEncoding("UTF-8");

        String step = model.get("step") != null ? (String) model.get("step") : "email";
        String errorMessage = (String) model.get("error");
        String token = (String) model.get("token");
        j2html.tags.DomContent csrfField = CsrfUtil.csrfInput(request);

        new YorkshireGolfPageTemplate().withRequest(request)
                .withTitle("Forgot Password — Yorkshire Golf Life")
                .withBody(
                        div().withClass("ygl-page-header").with(
                                div().withClass("container").with(
                                        h1("Forgot Password").withClass("ygl-page-header__title"),
                                        p("Reset your password via email.").withClass("ygl-page-header__lead")
                                )
                        ),
                        div().withClass("container ygl-page").with(
                                div().withClass("row justify-content-center").with(
                                        div().withClass("col-md-6 col-lg-5").with(
                                                errorMessage != null
                                                        ? div(errorMessage).withClass("alert alert-danger")
                                                        : text(""),
                                                buildStepContent(step, token, csrfField)
                                        )
                                )
                        )
                )
                .render(response.getWriter());
    }

    private static j2html.tags.DomContent buildStepContent(String step, String token, j2html.tags.DomContent csrfField) {
        return switch (step) {
            case "sent" -> buildSentStep();
            case "reset" -> buildResetStep(token, csrfField);
            case "done" -> buildDoneStep();
            default -> buildEmailStep(csrfField);
        };
    }

    private static j2html.tags.DomContent buildEmailStep(j2html.tags.DomContent csrfField) {
        return form().withMethod("post").withAction("/forgot-password").with(
                csrfField,
                input().withType("hidden").withName("step").withValue("email"),
                div().withClass("mb-3").with(
                        label("Your email address").withFor("email").withClass("form-label"),
                        input().withType("email").withId("email").withName("email")
                                .withClass("form-control").attr("required", "")
                ),
                button("Send Reset Link").withType("submit").withClass("btn ygl-btn ygl-btn--primary w-100")
        );
    }

    private static j2html.tags.DomContent buildSentStep() {
        return div().withClass("alert alert-success").with(
                p("If that email address is registered, you will receive a password reset link shortly.").withClass("mb-0"),
                p("The link is valid for 1 hour.").withClass("mb-0 mt-2 text-muted small")
        );
    }

    private static j2html.tags.DomContent buildResetStep(String token, j2html.tags.DomContent csrfField) {
        return form().withMethod("post").withAction("/forgot-password").with(
                csrfField,
                input().withType("hidden").withName("step").withValue("reset"),
                input().withType("hidden").withName("token").withValue(token != null ? token : ""),
                div().withClass("mb-3").with(
                        label("New Password").withFor("newPassword").withClass("form-label"),
                        input().withType("password").withId("newPassword").withName("newPassword")
                                .withClass("form-control").attr("required", "").attr("minlength", "8")
                                        .attr("pattern", "[A-Za-z0-9]+")
                                        .attr("title", "Letters and numbers only, at least 8 characters"),
                        div("Must be at least 8 characters, using letters and numbers only.").withClass("form-text")
                ),
                div().withClass("mb-3").with(
                        label("Confirm New Password").withFor("confirmPassword").withClass("form-label"),
                        input().withType("password").withId("confirmPassword").withName("confirmPassword")
                                .withClass("form-control").attr("required", "")
                ),
                button("Reset Password").withType("submit").withClass("btn ygl-btn ygl-btn--primary w-100")
        );
    }

    private static j2html.tags.DomContent buildDoneStep() {
        return div().withClass("text-center").with(
                p("Your password has been reset successfully.").withClass("mb-3"),
                a("Sign in").withClass("ygl-btn ygl-btn--primary").withHref("/login")
        );
    }

}

