package golf.web;

import golf.course.Courses;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CoursesController {

    private final Courses courses;

    @GetMapping
    public String courses(Model model) {
        model.addAttribute("courses", courses.getAllCourses());
        return "coursesPage";
    }

}
