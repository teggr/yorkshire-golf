package golf.course;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/next-100")
@RequiredArgsConstructor
public class Next100Controller {

    private final Courses courses;

    @GetMapping
    public String next100(Model model) {
        model.addAttribute("courses", courses.getNext100Courses());
        return "next100Page";
    }

}
