package golf.course;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/top-100")
@RequiredArgsConstructor
public class Top100Controller {

    private final Courses courses;

    @GetMapping
    public String top100(Model model) {
        model.addAttribute("courses", courses.getTop100Courses());
        return "top100Page";
    }

}
