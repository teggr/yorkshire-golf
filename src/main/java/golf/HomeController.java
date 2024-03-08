package golf;

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

    @RequestMapping("/")
    public String home(Model model) {
        log.info("Controller called - home");
        model.addAttribute("tracker",  yorkshireChallenge.getTracker());
        return "homePage";
    }

}
