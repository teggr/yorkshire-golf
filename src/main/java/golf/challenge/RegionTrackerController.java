package golf.challenge;

import golf.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/challenge")
@RequiredArgsConstructor
public class RegionTrackerController {

  private final UserService userService;

  @GetMapping
  public String home(@AuthenticationPrincipal UserDetails userDetails) {
    if (userDetails != null) {
      return userService.findByEmail(userDetails.getUsername())
              .map(user -> "redirect:/tracker/" + user.trackerId())
              .orElse("redirect:/login");
    }
    return "redirect:/login";
  }

}
