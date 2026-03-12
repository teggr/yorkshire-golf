package golf.user;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/forgot-password")
@RequiredArgsConstructor
public class ForgotPasswordController {

    private final UserService userService;
    private final ForgotPasswordRateLimiter forgotPasswordRateLimiter;

    @Value("${golf.app.base-url}")
    private String appBaseUrl;

    @GetMapping
    public String forgotPasswordPage(
            @RequestParam(required = false) String token,
            @RequestParam(required = false) String locked,
            Model model
    ) {
        if (token != null && !token.isBlank()) {
            model.addAttribute("step", "reset");
            model.addAttribute("token", token);
        } else {
            model.addAttribute("step", "email");
        }
        if ("true".equals(locked)) {
            model.addAttribute("locked", true);
        }
        return "forgotPasswordPage";
    }

    @PostMapping
    public String handleForgotPassword(
            @RequestParam String step,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String token,
            @RequestParam(required = false) String newPassword,
            @RequestParam(required = false) String confirmPassword,
            Model model
    ) {
        if ("email".equals(step)) {
            if (email != null && !email.isBlank()) {
                if (forgotPasswordRateLimiter.allow(email)) {
                    String resetBaseUrl = appBaseUrl + "/forgot-password";
                    userService.sendPasswordResetEmail(email, resetBaseUrl);
                }
            }
            model.addAttribute("step", "sent");
            return "forgotPasswordPage";
        }

        if ("reset".equals(step)) {
            if (token == null || token.isBlank()) {
                model.addAttribute("step", "email");
                model.addAttribute("error", "Invalid or missing reset token.");
                return "forgotPasswordPage";
            }

            var policyError = PasswordPolicy.validate(newPassword);
            if (policyError.isPresent()) {
                model.addAttribute("step", "reset");
                model.addAttribute("token", token);
                model.addAttribute("error", policyError.get());
                return "forgotPasswordPage";
            }

            if (!newPassword.equals(confirmPassword)) {
                model.addAttribute("step", "reset");
                model.addAttribute("token", token);
                model.addAttribute("error", "Passwords do not match.");
                return "forgotPasswordPage";
            }

            UserService.PasswordResetResult result = userService.resetPassword(token, newPassword);

            return switch (result) {
                case SUCCESS -> {
                    model.addAttribute("step", "done");
                    yield "forgotPasswordPage";
                }
                case EXPIRED_TOKEN -> {
                    model.addAttribute("step", "reset");
                    model.addAttribute("token", token);
                    model.addAttribute("error", "This reset link has expired. Please request a new one.");
                    yield "forgotPasswordPage";
                }
                case INVALID_TOKEN -> {
                    model.addAttribute("step", "email");
                    model.addAttribute("error", "This reset link is invalid or has already been used.");
                    yield "forgotPasswordPage";
                }
            };
        }

        return "redirect:/forgot-password";
    }

}
