package golf.course;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/play-and-stay")
@RequiredArgsConstructor
public class PlayAndStayController {

    private final Courses courses;

    @GetMapping
    public String playAndStay(Model model) {
        model.addAttribute("courses", courses.getPlayAndStayCourses());
        return "playAndStayPage";
    }

}
