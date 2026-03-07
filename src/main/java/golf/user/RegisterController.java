package golf.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/register")
@RequiredArgsConstructor
public class RegisterController {

    private final UserService userService;

    @GetMapping
    public String registerPage() {
        return "registerPage";
    }

    @PostMapping
    public String register(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam String securityQuestion,
            @RequestParam String securityAnswer,
            Model model
    ) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            model.addAttribute("email", email);
            model.addAttribute("securityQuestion", securityQuestion);
            return "registerPage";
        }

        if (password.length() < 8) {
            model.addAttribute("error", "Password must be at least 8 characters.");
            model.addAttribute("email", email);
            model.addAttribute("securityQuestion", securityQuestion);
            return "registerPage";
        }

        if (userService.emailExists(email)) {
            model.addAttribute("error", "An account with that email address already exists.");
            model.addAttribute("securityQuestion", securityQuestion);
            return "registerPage";
        }

        if (securityQuestion.isBlank() || securityAnswer.isBlank()) {
            model.addAttribute("error", "Security question and answer are required.");
            model.addAttribute("email", email);
            model.addAttribute("securityQuestion", securityQuestion);
            return "registerPage";
        }

        userService.register(email, password, securityQuestion, securityAnswer);
        return "redirect:/login?registered=true";
    }

}
