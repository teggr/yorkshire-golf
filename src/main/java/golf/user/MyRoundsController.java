package golf.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/my-rounds")
@RequiredArgsConstructor
public class MyRoundsController {

    private final UserService userService;
    private final UserRoundRepository userRoundRepository;

    @GetMapping
    public String myRounds(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        GolfUser user = resolveUser(userDetails);
        List<UserRound> rounds = userRoundRepository.findByUserId(user.id());
        model.addAttribute("userRounds", rounds);
        model.addAttribute("trackerId", user.trackerId());
        return "myRoundsPage";
    }

    @PostMapping("/add")
    public String addRound(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String courseName,
            @RequestParam String date,
            Model model
    ) {
        GolfUser user = resolveUser(userDetails);
        if (userRoundRepository.existsByUserIdAndCourseName(user.id(), courseName)) {
            model.addAttribute("error", "You have already logged a round at " + courseName + ".");
            model.addAttribute("userRounds", userRoundRepository.findByUserId(user.id()));
            model.addAttribute("trackerId", user.trackerId());
            return "myRoundsPage";
        }
        userRoundRepository.save(user.id(), courseName, date);
        return "redirect:/my-rounds?added=true";
    }

    @PostMapping("/edit/{id}")
    public String editRound(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestParam String date
    ) {
        GolfUser user = resolveUser(userDetails);
        userRoundRepository.updateDate(id, user.id(), date);
        return "redirect:/my-rounds";
    }

    @PostMapping("/delete/{id}")
    public String deleteRound(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        GolfUser user = resolveUser(userDetails);
        userRoundRepository.deleteByIdAndUserId(id, user.id());
        return "redirect:/my-rounds";
    }

    private GolfUser resolveUser(UserDetails userDetails) {
        return userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException(
                        "Logged in user not found in database: " + userDetails.getUsername()));
    }

}
