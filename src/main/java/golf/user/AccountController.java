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
        content.add(p("Link only accounts you control. Confirm sensitive changes with your current password, or a fresh sign-in for accounts without a password."));
        if (request.getParameter("error") != null) content.add(p("Unable to change sign-in methods. Please confirm your account and try again."));
        if (user.password() != null) content.add(p("Email and password sign-in is available."));
        List<ExternalIdentityService.Identity> linked = identities.list(user.id());
        for (var identity : linked) {
            String provider = identity.provider();
            content.add(h2(FederatedProviders.label(provider)));
            content.add(p(providers.enabled().contains(provider) ? "Linked" : "Linked (provider currently disabled)"));
            if (user.password() == null && providers.enabled().contains(provider)) {
                content.add(action(request, "reauthenticate", provider, "Confirm with " + FederatedProviders.label(provider), false));
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
                return "redirect:/account";
            }
            if (!providers.enabled().contains(provider)) throw new IllegalArgumentException();
            if ("link".equals(action)) {
                guard.requireConfirmation(user, password, request.getSession());
            } else if (identities.list(user.id()).stream().noneMatch(identity -> identity.provider().equals(provider))) {
                throw new IllegalArgumentException();
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
