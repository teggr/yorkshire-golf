package golf.user;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/login")
public class LoginController {

    @GetMapping
    public String loginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "existing", required = false) String existing,
            @RequestParam(value = "registered", required = false) String registered,
            @RequestParam(value = "deleted", required = false) String deleted,
            Model model
    ) {
        if (error != null) {
            model.addAttribute("error", true);
        }
        if (existing != null) {
            model.addAttribute("existing", true);
            model.addAttribute("existingProvider", switch (existing) {
                case "facebook" -> "Facebook";
                case "google", "true" -> "Google";
                default -> "your provider";
            });
        }
        if (registered != null) {
            model.addAttribute("registered", true);
        }
        if (deleted != null) {
            model.addAttribute("deleted", true);
        }
        return "loginPage";
    }

}
