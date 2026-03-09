package golf.course;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

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
            Course course = courses.getCourseBySlug(slug);
            model.addAttribute("course", course);
            model.addAttribute("nearbyCourses", resolveNearbyCourses(course));
            return "courseDetailPage";
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }
    }

    private List<Course> resolveNearbyCourses(Course course) {
        List<Course> nearbyCourses = new ArrayList<>();
        addNearbyCourseIfResolvable(nearbyCourses, course.nearby1());
        addNearbyCourseIfResolvable(nearbyCourses, course.nearby2());
        addNearbyCourseIfResolvable(nearbyCourses, course.nearby3());
        return nearbyCourses;
    }

    private void addNearbyCourseIfResolvable(List<Course> nearbyCourses, String nearbyCourseName) {
        if (nearbyCourseName == null || nearbyCourseName.isBlank()) {
            return;
        }

        try {
            Course nearbyCourse = courses.getCourseByName(nearbyCourseName);
            if (!nearbyCourse.closed()) {
                nearbyCourses.add(nearbyCourse);
            }
        } catch (RuntimeException ignored) {
            // Skip unresolved nearby course names so the parent detail page can still render.
        }
    }

}
