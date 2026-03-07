package golf.admin;

import golf.user.UserRepository;
import golf.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final UserService userService;

    @GetMapping
    public String adminPage(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "adminPage";
    }

    @PostMapping("/unlock/{userId}")
    public String unlockUser(@PathVariable Long userId) {
        userService.unlockAccount(userId);
        return "redirect:/admin";
    }

}
