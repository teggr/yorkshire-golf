package golf.user;

import golf.security.*;
import golf.utils.security.CsrfUtil;
import golf.web.YorkshireGolfPageTemplate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import j2html.tags.DomContent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final AccountDeletionService accountDeletion;
    private final ForgotPasswordRateLimiter forgotPasswordRateLimiter;

    public AccountController(UserService users, ExternalIdentityService identities,
                             FederatedProviders providers, AccountActionGuard guard,
                             AccountDeletionService accountDeletion,
                             ForgotPasswordRateLimiter forgotPasswordRateLimiter) {
        this.users = users;
        this.identities = identities;
        this.providers = providers;
        this.guard = guard;
        this.accountDeletion = accountDeletion;
        this.forgotPasswordRateLimiter = forgotPasswordRateLimiter;
    }

    @GetMapping
    public void account(Authentication authentication, HttpServletRequest request, HttpServletResponse response) throws Exception {
        GolfUser user = current(authentication);
        List<ExternalIdentityService.Identity> linked = identities.list(user.id());
        boolean canAddProvider = providers.enabled().stream()
                .anyMatch(provider -> linked.stream().noneMatch(identity -> identity.provider().equals(provider)));
        List<DomContent> content = new ArrayList<>();
        content.add(div().withClass("ygl-page-header").with(
                div().withClass("container").with(
                        h1("Sign-in methods").withClass("ygl-page-header__title"),
                        p("Manage the ways you can securely access your Yorkshire Golf Life account.")
                                .withClass("ygl-page-header__lead"))));
        String success = request.getParameter("success");
        String message = success == null ? null : switch (success) {
            case "linked" -> "Sign-in method linked successfully.";
            case "unlinked" -> "Sign-in method unlinked successfully.";
            case "reauthenticated" -> "Identity confirmed. You can now change a sign-in method or delete your account.";
            case "confirmed" -> "Account change confirmed for this session. You can now finish one change within five minutes.";
            default -> null;
        };
        List<DomContent> pageContent = new ArrayList<>();
        if (message != null) pageContent.add(p(message).withClass("alert alert-success").attr("role", "status"));
        if (request.getParameter("error") != null) pageContent.add(
                p("This account change could not be confirmed. No change was made; please try again.")
                        .withClass("alert alert-danger").attr("role", "alert"));
        if (user.password() != null) pageContent.add(p("Email and password sign-in is also available.")
                .withClass("ygl-sign-in-methods__notice"));

        pageContent.add(h2("Your sign-in methods").withClass("ygl-section-title"));
        pageContent.add(p("These methods all open the same account and keep your tracker and rounds together."));
        for (var identity : linked) {
            String provider = identity.provider();
            boolean available = providers.enabled().contains(provider);
            boolean canUnlink = canUnlink(user, identity, linked);
            boolean canConfirm = user.password() == null && available;
            List<DomContent> actions = new ArrayList<>();
            if (canConfirm) {
                actions.add(action(request, "reauthenticate", provider, "Confirm with " + FederatedProviders.label(provider), false));
            }
            if (canUnlink) actions.add(action(request, "unlink", provider, "Unlink " + FederatedProviders.label(provider),
                    user.password() != null));
            pageContent.add(div().withClass("ygl-sign-in-method-card").with(
                    div().withClass("ygl-sign-in-method-card__identity").with(
                            img().withSrc("/images/auth/" + provider + ".svg").withAlt("")
                                    .attr("aria-hidden", "true").attr("width", "32").attr("height", "32"),
                            div().with(
                                    h3(FederatedProviders.label(provider)).withClass("ygl-sign-in-method-card__title"),
                                    p(available ? "Linked" : "Linked, but sign-in is currently unavailable")
                                            .withClass(available ? "ygl-sign-in-method-card__status"
                                                    : "ygl-sign-in-method-card__status ygl-sign-in-method-card__status--muted"))),
                    div().withClass("ygl-sign-in-method-card__actions").with(actions)));
        }

        List<String> unlinked = providers.enabled().stream()
                .filter(provider -> linked.stream().noneMatch(identity -> identity.provider().equals(provider))).toList();
        if (!unlinked.isEmpty()) {
            pageContent.add(h2("Add another sign-in method").withClass("ygl-section-title ygl-section-title--spaced"));
            pageContent.add(p(user.password() == null
                    ? "Confirm one of your linked accounts first, then you can connect another provider to this account."
                    : "Link only accounts you control. Confirm with your current password to add a provider."));
            List<DomContent> availableActions = new ArrayList<>();
            for (String provider : unlinked) {
                availableActions.add(action(request, "link", provider, "Link " + FederatedProviders.label(provider),
                        user.password() != null));
            }
            pageContent.add(div().withClass("ygl-sign-in-methods__available").with(availableActions));
        } else if (providers.enabled().isEmpty()) {
            pageContent.add(p("No external sign-in providers are currently available.")
                    .withClass("ygl-sign-in-methods__notice"));
        }
        if (linked.size() == 1 && user.password() == null && !canAddProvider) {
            pageContent.add(p("This is your only sign-in method, so it cannot be unlinked while keeping your account. You can still delete the full account below.")
                    .withClass("ygl-sign-in-methods__notice").attr("role", "status"));
        }
        List<DomContent> deletionContent = new ArrayList<>();
        deletionContent.add(h2("Delete account").withClass("ygl-section-title"));
        deletionContent.add(p("Permanently delete your account, linked sign-in methods, tracker, and all recorded golf rounds. This cannot be undone.")
                .withClass("ygl-sign-in-methods__notice"));
        if (user.password() == null) {
            deletionContent.add(p("Confirm your identity with one of your linked sign-in providers before deleting your account.")
                    .withClass("ygl-sign-in-methods__notice"));
        }
        deletionContent.add(form().withMethod("post").withAction("/account/delete")
                .attr("hx-boost", "false").withClass("ygl-sign-in-action").with(
                        CsrfUtil.csrfInput(request),
                        user.password() != null
                                ? label("Current password").withClass("ygl-sign-in-action__password").with(
                                        input().withType("password").withName("password").withClass("form-control")
                                                .attr("autocomplete", "current-password").attr("required"))
                                : text(""),
                        label().withClass("form-check ygl-sign-in-action__confirm").with(
                                input().withType("checkbox").withName("confirmed").withValue("true")
                                        .withClass("form-check-input").attr("required"),
                                span("I understand this permanently deletes my account and all golf rounds.")
                                        .withClass("form-check-label")),
                        button("Delete my account").withType("submit").withClass("btn btn-danger")
                ));
        pageContent.add(div().withClass("ygl-sign-in-methods__danger").with(deletionContent));
        content.add(div().withClass("container ygl-page").with(
                div().withClass("row justify-content-center").with(
                        div().withClass("col-lg-9 ygl-sign-in-methods").with(pageContent))));
        response.setContentType("text/html;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        new YorkshireGolfPageTemplate().withRequest(request).withTitle("Sign-in methods — Yorkshire Golf Life")
                .withBody(content.toArray(DomContent[]::new))
                .render(response.getWriter());
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

    @PostMapping("/delete")
    public String delete(@RequestParam(defaultValue = "false") boolean confirmed,
                         @RequestParam(required = false) String password,
                         Authentication authentication, HttpServletRequest request) {
        try {
            GolfUser user = current(authentication);
            guard.rateLimit(user.id());
            if (!confirmed) throw new IllegalArgumentException();
            guard.requireConfirmation(user, password, request.getSession());
            accountDeletion.delete(user.id());
            guard.forget(user.id());
            forgotPasswordRateLimiter.forget(user.email());
        } catch (IllegalArgumentException ex) {
            return "redirect:/account?error=true";
        }
        SecurityContextHolder.clearContext();
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        return "redirect:/login?deleted=true";
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

    private boolean canUnlink(GolfUser user, ExternalIdentityService.Identity target,
                              List<ExternalIdentityService.Identity> linked) {
        return user.password() != null || linked.stream()
                .filter(identity -> !identity.provider().equals(target.provider()))
                .anyMatch(identity -> providers.enabled().contains(identity.provider()));
    }

    private DomContent action(HttpServletRequest request, String action, String provider, String label, boolean password) {
        DomContent submit = "link".equals(action)
                ? button().with(
                        img().withSrc("/images/auth/" + provider + ".svg").withAlt("")
                                .attr("aria-hidden", "true").attr("width", "20").attr("height", "20"),
                        span(label))
                        .withType("submit")
                        .withClass("btn ygl-btn ygl-provider-sign-in ygl-provider-sign-in--" + provider)
                : button(label).withType("submit").withClass("btn ygl-btn ygl-btn--outline");
        return form().withMethod("post").withAction("/account/" + action + "/" + provider)
                .attr("hx-boost", "false").withClass("ygl-sign-in-action").with(
                        CsrfUtil.csrfInput(request),
                        password ? label("Current password").withClass("ygl-sign-in-action__password").with(
                                input().withType("password").withName("password").withClass("form-control")
                                        .attr("autocomplete", "current-password").attr("required")) : text(""),
                        label().withClass("form-check ygl-sign-in-action__confirm").with(
                                input().withType("checkbox").withName("confirmed").withValue("true")
                                        .withClass("form-check-input").attr("required"),
                                span("I confirm this change").withClass("form-check-label")),
                        submit);
    }
}
