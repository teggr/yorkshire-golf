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
        String step = model.get("step") != null ? (String) model.get("step") : "email";
        String errorMessage = (String) model.get("error");
        String successMessage = (String) model.get("success");
        String email = model.get("email") != null ? (String) model.get("email") : "";
        String securityQuestion = (String) model.get("securityQuestion");
        int remainingAttempts = model.containsKey("remainingAttempts")
                ? (Integer) model.get("remainingAttempts") : 3;
        j2html.tags.DomContent csrfField = CsrfUtil.csrfInput(request);

        new YorkshireGolfPageTemplate().withRequest(request)
                .withTitle("Forgot Password — Yorkshire Golf Life")
                .withBody(
                        div().withClass("ygl-page-header").with(
                                div().withClass("container").with(
                                        h1("Forgot Password").withClass("ygl-page-header__title"),
                                        p("Answer your security question to reset your password.").withClass("ygl-page-header__lead")
                                )
                        ),
                        div().withClass("container ygl-page").with(
                                div().withClass("row justify-content-center").with(
                                        div().withClass("col-md-6 col-lg-5").with(
                                                errorMessage != null
                                                        ? div(errorMessage).withClass("alert alert-danger")
                                                        : text(""),
                                                successMessage != null
                                                        ? div(successMessage).withClass("alert alert-success")
                                                        : text(""),
                                                buildStepContent(step, email, securityQuestion, remainingAttempts, csrfField)
                                        )
                                )
                        )
                )
                .render(response.getWriter());
    }

    private static j2html.tags.DomContent buildStepContent(String step, String email, String securityQuestion, int remainingAttempts, j2html.tags.DomContent csrfField) {
        return switch (step) {
            case "question" -> buildQuestionStep(email, securityQuestion, remainingAttempts, csrfField);
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
                button("Continue").withType("submit").withClass("btn ygl-btn ygl-btn--primary w-100")
        );
    }

    private static j2html.tags.DomContent buildQuestionStep(String email, String securityQuestion, int remainingAttempts, j2html.tags.DomContent csrfField) {
        return div().with(
                p("Security question for: ").withClass("text-muted small").with(strong(email)),
                div().withClass("alert alert-info mb-3").with(
                        strong(securityQuestion != null ? securityQuestion : "")
                ),
                p("You have " + remainingAttempts + " attempt(s) remaining.").withClass("text-muted small"),
                form().withMethod("post").withAction("/forgot-password").with(
                        csrfField,
                        input().withType("hidden").withName("step").withValue("answer"),
                        input().withType("hidden").withName("email").withValue(email),
                        div().withClass("mb-3").with(
                                label("Your answer").withFor("answer").withClass("form-label"),
                                input().withType("text").withId("answer").withName("answer")
                                        .withClass("form-control").attr("required", "")
                        ),
                        div().withClass("mb-3").with(
                                label("New Password").withFor("newPassword").withClass("form-label"),
                                input().withType("password").withId("newPassword").withName("newPassword")
                                        .withClass("form-control").attr("required", "").attr("minlength", "8")
                        ),
                        div().withClass("mb-3").with(
                                label("Confirm New Password").withFor("confirmPassword").withClass("form-label"),
                                input().withType("password").withId("confirmPassword").withName("confirmPassword")
                                        .withClass("form-control").attr("required", "")
                        ),
                        button("Reset Password").withType("submit").withClass("btn ygl-btn ygl-btn--primary w-100")
                )
        );
    }

    private static j2html.tags.DomContent buildDoneStep() {
        return div().withClass("text-center").with(
                p("Your password has been reset successfully.").withClass("mb-3"),
                a("Sign in").withClass("ygl-btn ygl-btn--primary").withHref("/login")
        );
    }

}

