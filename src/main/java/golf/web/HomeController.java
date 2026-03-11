package golf.web;

import golf.course.Course;
import golf.course.Courses;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Random;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class HomeController {

  private final Courses courses;

  @GetMapping
  public String home(Model model) {
    List<Course> allCourses = courses.getAllCourses();
    if (allCourses.isEmpty()) {
      model.addAttribute("courses", allCourses);
      return "homePage";
    }
    Course featured = allCourses.get(new Random().nextInt(allCourses.size()));
    model.addAttribute("featuredCourse", featured);
    model.addAttribute("courses", allCourses);
    return "homePage";
  }

}
