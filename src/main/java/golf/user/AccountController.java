package golf.user;

import golf.security.*;
import golf.utils.security.CsrfUtil;
import golf.web.YorkshireGolfPageTemplate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import j2html.tags.DomContent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static j2html.TagCreator.*;

@Controller
@RequestMapping("/account")
public class AccountController {
    private final UserService users;
    private final ExternalIdentityService identities;
    private final FederatedProviders providers;
    private final AccountActionGuard guard;

    public AccountController(UserService users, ExternalIdentityService identities,
                             FederatedProviders providers, AccountActionGuard guard) {
        this.users = users;
        this.identities = identities;
        this.providers = providers;
        this.guard = guard;
    }

    @GetMapping
    public void account(Authentication authentication, HttpServletRequest request, HttpServletResponse response) throws Exception {
        GolfUser user = current(authentication);
        List<DomContent> content = new ArrayList<>();
        content.add(h1("Sign-in methods"));
        content.add(p("Link only accounts you control. Confirm sensitive changes with your current password or the confirmation method shown below."));
        if (appleOnlyConfirmation(user)) content.add(p(
                "Confirm control of your linked Apple account to allow one change within five minutes. Apple may reuse its existing sign-in session; this does not force credential re-entry."));
        String success = request.getParameter("success");
        String message = success == null ? null : switch (success) {
            case "linked" -> "Sign-in method linked successfully.";
            case "unlinked" -> "Sign-in method unlinked successfully.";
            case "reauthenticated" -> "Identity confirmed. You can now finish linking or unlinking a sign-in method.";
            case "confirmed" -> "Account change confirmed for this session. You can now finish one change within five minutes.";
            default -> null;
        };
        if (message != null) content.add(p(message).withClass("alert alert-success").attr("role", "status"));
        if (request.getParameter("error") != null) content.add(
                p("Unable to change sign-in methods. Please confirm your account and try again.")
                        .withClass("alert alert-danger").attr("role", "alert"));
        if (user.password() != null) content.add(p("Email and password sign-in is available."));
        List<ExternalIdentityService.Identity> linked = identities.list(user.id());
        for (var identity : linked) {
            String provider = identity.provider();
            content.add(h2(FederatedProviders.label(provider)));
            content.add(p(providers.enabled().contains(provider) ? "Linked" : "Linked (provider currently disabled)"));
            if (user.password() == null && providers.enabled().contains(provider)) {
                if ("apple".equals(provider)) {
                    if (appleOnlyConfirmation(user)) content.add(action(request, "reauthenticate", provider, "Confirm with Apple", false));
                } else {
                    content.add(action(request, "reauthenticate", provider, "Confirm with " + FederatedProviders.label(provider), false));
                }
            }
            content.add(action(request, "unlink", provider, "Unlink " + FederatedProviders.label(provider), user.password() != null));
        }
        for (String provider : providers.enabled()) {
            if (linked.stream().noneMatch(identity -> identity.provider().equals(provider))) {
                content.add(action(request, "link", provider, "Link " + FederatedProviders.label(provider), user.password() != null));
            }
        }
        content.add(a("Challenge Tracker").withHref("/challenge"));
        response.setContentType("text/html;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        new YorkshireGolfPageTemplate().withRequest(request).withTitle("Sign-in methods — Yorkshire Golf Life")
                .withBody(div().withClass("container ygl-page").with(content)).render(response.getWriter());
    }

    @PostMapping("/{action}/{provider}")
    public String change(@PathVariable String action, @PathVariable String provider,
                         @RequestParam(required = false) String password,
                         @RequestParam(defaultValue = "false") boolean confirmed,
                         Authentication authentication, HttpServletRequest request) {
        try {
            GolfUser user = current(authentication);
            guard.rateLimit(user.id());
            if (!confirmed) throw new IllegalArgumentException();
            if (!List.of("link", "unlink", "reauthenticate").contains(action)) throw new IllegalArgumentException();
            if ("unlink".equals(action)) {
                guard.requireConfirmation(user, password, request.getSession());
                identities.unlink(user.id(), provider, providers.enabled());
                return "redirect:/account?success=unlinked";
            }
            if (!providers.enabled().contains(provider)) throw new IllegalArgumentException();
            if ("link".equals(action)) {
                guard.requireConfirmation(user, password, request.getSession());
            } else if (identities.list(user.id()).stream().noneMatch(identity -> identity.provider().equals(provider))) {
                throw new IllegalArgumentException();
            }
            if ("reauthenticate".equals(action) && "apple".equals(provider)) {
                if (!appleOnlyConfirmation(user)) throw new IllegalArgumentException();
            }
            request.getSession().setAttribute(FederatedFlow.SESSION_KEY,
                    new FederatedFlow(action, provider, user.id(), Instant.now().plusSeconds(300)));
            return "redirect:/oauth2/authorization/" + provider;
        } catch (RuntimeException ex) {
            return "redirect:/account?error=true";
        }
    }

    private GolfUser current(Authentication authentication) {
        return users.findByPrincipal(authentication).map(user -> identities.usable(user.id()))
                .orElseThrow(() -> new IllegalArgumentException("Account unavailable"));
    }

    private boolean appleOnlyConfirmation(GolfUser user) {
        if (user.password() != null) return false;
        var usable = identities.list(user.id()).stream().map(ExternalIdentityService.Identity::provider)
                .filter(providers.enabled()::contains).toList();
        return usable.equals(List.of("apple"));
    }

    private DomContent action(HttpServletRequest request, String action, String provider, String label, boolean password) {
        return form().withMethod("post").withAction("/account/" + action + "/" + provider).attr("hx-boost", "false").with(
                CsrfUtil.csrfInput(request),
                password ? label("Current password").with(input().withType("password").withName("password")
                        .attr("autocomplete", "current-password").attr("required")) : text(""),
                label().with(input().withType("checkbox").withName("confirmed").withValue("true").attr("required"),
                        text(" I confirm this change")),
                button(label).withType("submit").withClass("btn btn-primary mb-3"));
    }
}
