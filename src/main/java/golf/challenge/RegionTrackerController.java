package golf.challenge;

import golf.course.Courses;
import golf.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/challenge")
@RequiredArgsConstructor
public class RegionTrackerController {

  private final UserService userService;
  private final Courses courses;

  @GetMapping
  public String home(@AuthenticationPrincipal UserDetails userDetails, Model model) {
    if (userDetails != null) {
      return (userDetails instanceof golf.security.LocalPrincipal local
              ? userService.findById(local.userId()) : userService.findByEmail(userDetails.getUsername()))
              .map(user -> "redirect:/challenge/" + user.trackerId())
              .orElse("redirect:/login");
    }
    model.addAttribute("totalCourseCount", courses.getAllCourses().size());
    return "challengeLandingPage";
  }

}
