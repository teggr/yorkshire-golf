package golf.web;

import golf.tracker.Courses;
import golf.tracker.YorkshireChallenge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@Slf4j
class HomeController {

    private final YorkshireChallenge yorkshireChallenge;
    private final Courses courses;

    @RequestMapping("/")
    public String home(Model model) {
        // TODO: logged in - go to dashboard
        // TODO: not logged in - yorkshire challenge home page
        log.info("Controller called - home");
//        model.addAttribute("courses", courses.getAllCourses());
//        return "yorkshireGolfChallengePage";
        model.addAttribute("tracker",  yorkshireChallenge.getTracker());
        return "homePage";
    }

}
