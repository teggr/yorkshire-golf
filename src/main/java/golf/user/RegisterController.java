package golf.user;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/register")
public class RegisterController {
    private final UserService userService;
    private final LocalRegistration localRegistration;

    public RegisterController(UserService userService, LocalRegistration localRegistration) {
        this.userService = userService;
        this.localRegistration = localRegistration;
    }

    @GetMapping
    public String registerPage(@RequestParam(defaultValue = "false") boolean providerRequired, Model model) {
        model.addAttribute("providerRequired", providerRequired);
        model.addAttribute("localRegistrationEnabled", localRegistration.enabled());
        return "registerPage";
    }

    @PostMapping
    public String register(@RequestParam(required = false) String email,
                           @RequestParam(required = false) String password,
                           @RequestParam(required = false) String confirmPassword,
                           Model model) {
        if (!localRegistration.enabled()) return "redirect:/register?providerRequired=true";
        if (email == null || email.isBlank() || password == null || confirmPassword == null) {
            model.addAttribute("error", "Enter your email address and both password fields.");
            model.addAttribute("email", email == null ? "" : email);
            model.addAttribute("localRegistrationEnabled", true);
            return "registerPage";
        }
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            model.addAttribute("email", email);
            model.addAttribute("localRegistrationEnabled", true);
            return "registerPage";
        }

        var policyError = PasswordPolicy.validate(password);
        if (policyError.isPresent()) {
            model.addAttribute("error", policyError.get());
            model.addAttribute("email", email);
            model.addAttribute("localRegistrationEnabled", true);
            return "registerPage";
        }

        if (userService.emailExists(email)) {
            model.addAttribute("error", "An account with that email address already exists.");
            model.addAttribute("email", email);
            model.addAttribute("localRegistrationEnabled", true);
            return "registerPage";
        }

        try {
            userService.register(email, password);
        } catch (DuplicateEmailException ex) {
            model.addAttribute("error", "An account with that email address already exists.");
            model.addAttribute("email", email);
            model.addAttribute("localRegistrationEnabled", true);
            return "registerPage";
        }
        return "redirect:/login?registered=true";
    }

}
