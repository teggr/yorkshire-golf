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
            Model model
    ) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            model.addAttribute("email", email);
            return "registerPage";
        }

        var policyError = PasswordPolicy.validate(password);
        if (policyError.isPresent()) {
            model.addAttribute("error", policyError.get());
            model.addAttribute("email", email);
            return "registerPage";
        }

        if (userService.emailExists(email)) {
            model.addAttribute("error", "An account with that email address already exists.");
            return "registerPage";
        }

        userService.register(email, password);
        return "redirect:/login?registered=true";
    }

}
