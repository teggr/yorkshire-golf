package golf.challenge;

import golf.user.GolfUser;
import golf.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/tracker")
@RequiredArgsConstructor
public class UserTrackerController {

    private final UserService userService;
    private final YorkshireChallenge yorkshireChallenge;

    @GetMapping("/{trackerId}")
    public String tracker(@PathVariable String trackerId, Model model) {
        GolfUser user = userService.findByTrackerId(trackerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tracker not found"));
        model.addAttribute("tracker", yorkshireChallenge.getTrackerForUser(user.id()));
        return "regionTrackerPage";
    }

}
