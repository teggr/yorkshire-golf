package golf.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.View;

import java.util.Map;

import static j2html.TagCreator.*;

@Component
public class DataDeletionPage implements View {
    @Override
    public @Nullable String getContentType() {
        return MediaType.TEXT_HTML_VALUE;
    }

    @Override
    public void render(@Nullable Map<String, ?> model, HttpServletRequest request,
                       HttpServletResponse response) throws Exception {
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.setCharacterEncoding("UTF-8");
        new YorkshireGolfPageTemplate().withRequest(request)
                .withTitle("Data deletion — Yorkshire Golf Life")
                .withDescription("How to delete your Yorkshire Golf Life account and associated data.")
                .withBody(
                        div().withClass("ygl-page-header").with(
                                div().withClass("container").with(
                                        h1("Delete your Yorkshire Golf Life account").withClass("ygl-page-header__title"),
                                        p("You can remove your account and its associated data at any time.")
                                                .withClass("ygl-page-header__lead")
                                )
                        ),
                        div().withClass("container ygl-page").with(
                                h2("Delete your account"),
                                p("Sign in, open Sign-in methods, and use Delete account. Email-and-password accounts confirm with their current password. Accounts that use an external provider confirm by signing in again with a linked provider."),
                                p("Account deletion is permanent. It removes your Yorkshire Golf Life account, linked sign-in identifiers, tracker ID, password-reset records, and all golf rounds associated with the account from the live service."),
                                p("Backup copies, if any, may remain according to infrastructure retention processes. We do not state a specific backup-erasure deadline."),
                                h2("If you cannot sign in"),
                                p().with(text("Email "), a("robin@tegg.me.uk").withHref("mailto:robin@tegg.me.uk"),
                                        text(" with an account-deletion request. We may ask you to verify that you control the account before processing the request. Do not send your password, Facebook password, or sign-in tokens.")),
                                p("Facebook sign-in uses a Facebook app-scoped identifier. We do not store Facebook email addresses, names, profile photos, or access tokens. Deleting your Yorkshire Golf Life account removes the stored Facebook identifier along with the account."),
                                p().with(text("Read our "), a("privacy notice").withHref("/privacy"), text(".")),
                                a("Sign in to manage your account").withHref("/login").withClass("btn ygl-btn ygl-btn--primary")
                        )
                )
                .render(response.getWriter());
    }
}
