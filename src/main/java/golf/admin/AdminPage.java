package golf.admin;

import golf.user.GolfUser;
import golf.utils.security.CsrfUtil;
import golf.web.YorkshireGolfPageTemplate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.View;

import java.util.List;
import java.util.Map;

import static j2html.TagCreator.*;

@Component
public class AdminPage implements View {

    @Override
    public @Nullable String getContentType() {
        return MediaType.TEXT_HTML_VALUE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<GolfUser> users = model.get("users") != null ? (List<GolfUser>) model.get("users") : List.of();
        j2html.tags.DomContent csrfField = CsrfUtil.csrfInput(request);

        new YorkshireGolfPageTemplate().withRequest(request)
                .withTitle("Admin — Yorkshire Golf Life")
                .withBody(
                        div().withClass("ygl-page-header").with(
                                div().withClass("container").with(
                                        h1("Admin").withClass("ygl-page-header__title")
                                )
                        ),
                        div().withClass("container ygl-page").with(
                                h2("User Management").withClass("mb-4"),
                                div().withClass("table-responsive").with(
                                        table().withClass("table table-hover align-middle").with(
                                                thead().with(
                                                        tr().with(
                                                                th("Email"),
                                                                th("Role"),
                                                                th("Status"),
                                                                th("Failed Attempts"),
                                                                th("Actions")
                                                        )
                                                ),
                                                tbody().with(
                                                        each(users, user ->
                                                                tr().with(
                                                                        td(user.email()),
                                                                        td(user.role()),
                                                                        td(user.accountLocked() ? "LOCKED" : "Active"),
                                                                        td(String.valueOf(user.failedSecurityAttempts())),
                                                                        td().with(
                                                                                user.accountLocked()
                                                                                        ? form().withMethod("post").withAction("/admin/unlock/" + user.id()).with(
                                                                                                csrfField,
                                                                                                button("Unlock").withType("submit").withClass("btn btn-sm btn-success")
                                                                                          )
                                                                                        : text("—")
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
                .render(response.getWriter());
    }

}
