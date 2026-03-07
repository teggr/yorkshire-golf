package golf.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequestMapping("/forgot-password")
@RequiredArgsConstructor
public class ForgotPasswordController {

    private final UserService userService;

    @GetMapping
    public String forgotPasswordPage(Model model) {
        model.addAttribute("step", "email");
        return "forgotPasswordPage";
    }

    @PostMapping
    public String handleForgotPassword(
            @RequestParam String step,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String answer,
            @RequestParam(required = false) String newPassword,
            @RequestParam(required = false) String confirmPassword,
            Model model
    ) {
        if ("email".equals(step)) {
            Optional<GolfUser> userOpt = userService.findByEmail(email);
            if (userOpt.isEmpty()) {
                // Don't reveal whether email exists — show question step with a generic error
                model.addAttribute("step", "email");
                model.addAttribute("error", "If that email address is registered, you will be prompted for your security question.");
                return "forgotPasswordPage";
            }
            GolfUser user = userOpt.get();
            if (user.accountLocked()) {
                model.addAttribute("step", "email");
                model.addAttribute("error", "This account is locked. Please contact an administrator.");
                return "forgotPasswordPage";
            }
            model.addAttribute("step", "question");
            model.addAttribute("email", email);
            model.addAttribute("securityQuestion", user.securityQuestion());
            model.addAttribute("remainingAttempts", userService.remainingSecurityAttempts(user));
            return "forgotPasswordPage";
        }

        if ("answer".equals(step)) {
            Optional<GolfUser> userOpt = userService.findByEmail(email);
            if (userOpt.isEmpty()) {
                model.addAttribute("step", "email");
                model.addAttribute("error", "Session expired, please try again.");
                return "forgotPasswordPage";
            }
            GolfUser user = userOpt.get();

            if (newPassword == null || newPassword.length() < 8) {
                model.addAttribute("step", "question");
                model.addAttribute("email", email);
                model.addAttribute("securityQuestion", user.securityQuestion());
                model.addAttribute("remainingAttempts", userService.remainingSecurityAttempts(user));
                model.addAttribute("error", "New password must be at least 8 characters.");
                return "forgotPasswordPage";
            }

            if (!newPassword.equals(confirmPassword)) {
                model.addAttribute("step", "question");
                model.addAttribute("email", email);
                model.addAttribute("securityQuestion", user.securityQuestion());
                model.addAttribute("remainingAttempts", userService.remainingSecurityAttempts(user));
                model.addAttribute("error", "Passwords do not match.");
                return "forgotPasswordPage";
            }

            UserService.SecurityAnswerResult result = userService.attemptSecurityAnswer(user, answer, newPassword);

            return switch (result) {
                case SUCCESS -> {
                    model.addAttribute("step", "done");
                    model.addAttribute("success", "Your password has been reset successfully.");
                    yield "forgotPasswordPage";
                }
                case WRONG_ANSWER -> {
                    // Reload user to get updated attempt count
                    GolfUser refreshed = userService.findByEmail(email).orElse(user);
                    model.addAttribute("step", "question");
                    model.addAttribute("email", email);
                    model.addAttribute("securityQuestion", refreshed.securityQuestion());
                    model.addAttribute("remainingAttempts", userService.remainingSecurityAttempts(refreshed));
                    model.addAttribute("error", "Incorrect answer. Please try again.");
                    yield "forgotPasswordPage";
                }
                case ACCOUNT_LOCKED -> {
                    model.addAttribute("step", "email");
                    model.addAttribute("error", "Your account has been locked after too many failed attempts. Please contact an administrator.");
                    yield "forgotPasswordPage";
                }
            };
        }

        return "redirect:/forgot-password";
    }

}
