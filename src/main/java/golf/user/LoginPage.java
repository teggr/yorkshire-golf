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

    @Override
    public @Nullable String getContentType() {
        return MediaType.TEXT_HTML_VALUE;
    }

    @Override
    public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
                response.setContentType(MediaType.TEXT_HTML_VALUE);
                response.setCharacterEncoding("UTF-8");

        boolean hasError = Boolean.TRUE.equals(model.get("error"));
        boolean registered = Boolean.TRUE.equals(model.get("registered"));

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
                                                hasError
                                                        ? div("Invalid email or password. Your account may also be locked.").withClass("alert alert-danger")
                                                        : text(""),
                                                registered
                                                        ? div("Account created successfully! Please sign in.").withClass("alert alert-success")
                                                        : text(""),
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
