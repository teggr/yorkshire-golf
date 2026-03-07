package golf.course;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

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

    @GetMapping("/{slug}")
    public String course(@PathVariable String slug, Model model) {
        try {
            model.addAttribute("course", courses.getCourseBySlug(slug));
            return "courseDetailPage";
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }
    }

}
