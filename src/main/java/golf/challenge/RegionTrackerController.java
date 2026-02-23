package golf.challenge;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/challenge")
@RequiredArgsConstructor
public class RegionTrackerController {

  private final YorkshireChallenge yorkshireChallenge;

  @GetMapping
  public String home(Model model) {
    model.addAttribute("tracker", yorkshireChallenge.getTracker());
    return "homePage";
  }

}
