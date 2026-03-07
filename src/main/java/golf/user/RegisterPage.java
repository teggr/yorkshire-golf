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
public class RegisterPage implements View {

    @Override
    public @Nullable String getContentType() {
        return MediaType.TEXT_HTML_VALUE;
    }

    @Override
    public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String errorMessage = (String) model.get("error");
        String emailValue = model.get("email") != null ? (String) model.get("email") : "";

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
                                                        ? div(errorMessage).withClass("alert alert-danger")
                                                        : text(""),
                                                form().withMethod("post").withAction("/register").with(
                                                        CsrfUtil.csrfInput(request),
                                                        div().withClass("mb-3").with(
                                                                label("Email address").withFor("email").withClass("form-label"),
                                                                input().withType("email").withId("email").withName("email")
                                                                        .withValue(emailValue)
                                                                        .withClass("form-control").attr("required", "")
                                                        ),
                                                        div().withClass("mb-3").with(
                                                                label("Password").withFor("password").withClass("form-label"),
                                                                input().withType("password").withId("password").withName("password")
                                                                        .withClass("form-control").attr("required", "")
                                                                        .attr("minlength", "8")
                                                        ),
                                                        div().withClass("mb-3").with(
                                                                label("Confirm Password").withFor("confirmPassword").withClass("form-label"),
                                                                input().withType("password").withId("confirmPassword").withName("confirmPassword")
                                                                        .withClass("form-control").attr("required", "")
                                                        ),
                                                        button("Create Account").withType("submit").withClass("btn ygl-btn ygl-btn--primary w-100 mb-3")
                                                ),
                                                div().withClass("text-center").with(
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

}
