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
public class PrivacyPage implements View {
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
                .withTitle("Privacy — Yorkshire Golf Life")
                .withDescription("How Yorkshire Golf Life uses sign-in and Challenge Tracker data.")
                .withBody(
                        div().withClass("ygl-page-header").with(
                                div().withClass("container").with(
                                        h1("Privacy at Yorkshire Golf Life").withClass("ygl-page-header__title"),
                                        p("How we use and protect account and Challenge Tracker information.")
                                                .withClass("ygl-page-header__lead")
                                )
                        ),
                        div().withClass("container ygl-page").with(
                                h2("Information we use"),
                                p("When you sign in with Facebook, Google, Apple or X, we use only the information needed to connect that sign-in to your Yorkshire Golf Life account. When Facebook or Google provides an email address, we check it temporarily to help prevent duplicate accounts. We do not store provider email addresses, names, profile photos or access tokens, and we never automatically link accounts by email."),
                                p("We are actively moving away from email-and-password accounts. Existing members can continue to use their current account during this transition and can link a trusted sign-in provider from Sign-in methods. We also keep your Challenge Tracker and the golf courses and dates you record."),
                                h2("How we use it"),
                                p("We use account information to authenticate you, secure access, manage sign-in methods, and show your Challenge Tracker and recorded rounds. Tracker pages are public at their shareable URLs; they show course progress and recorded course names and dates."),
                                h2("Cookies and service providers"),
                                p("Essential session cookies keep you signed in and protect account actions. The site loads Simple Analytics for aggregate website-usage measurement. The map page may connect your browser to Google Maps. Password-reset emails, when used, are sent through our configured email provider."),
                                p("When you choose an external sign-in provider, that provider handles its own authentication and privacy practices. Yorkshire Golf Life stores only the provider name and the provider-specific account identifier needed to recognize your sign-in; it does not retain provider tokens or profile details."),
                                h2("Retention and deletion"),
                                p("We keep account and tracker data while the account remains active. You can delete your account from Sign-in methods after confirming your identity. Deletion removes the account, linked sign-in identifiers, password-reset records, tracker, and rounds from the live service. Backup copies, if any, may remain according to infrastructure retention processes; we do not state a specific backup-erasure deadline."),
                                h2("Contact"),
                                p().with(text("For privacy questions or help deleting an account you cannot access, contact "),
                                        a("robin@tegg.me.uk").withHref("mailto:robin@tegg.me.uk"), text(".")),
                                p().with(text("See also the "),
                                        a("data deletion instructions").withHref("/data-deletion"), text("."))
                        )
                )
                .render(response.getWriter());
    }
}
