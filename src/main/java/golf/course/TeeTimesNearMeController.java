package golf.course;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping({"/tee-times-near-me"})
public class TeeTimesNearMeController {

    @GetMapping
    public String teeTimesNearMe() {
        return "redirect:https://www.golfnow.co.uk/tee-times/courses-near-me";
    }
}