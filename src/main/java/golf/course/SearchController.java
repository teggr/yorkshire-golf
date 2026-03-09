package golf.course;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final Courses courses;

    @GetMapping
    public String search(@RequestParam(required = false) String q, Model model) {
        List<Course> results = courses.search(q);

        if (results.size() == 1) {
            Course match = results.get(0);
            return "redirect:/courses/" + Courses.toCourseSlug(match.name());
        }

        model.addAttribute("query", q == null ? "" : q.trim());
        model.addAttribute("results", results);
        model.addAttribute("resultCount", results.size());
        return "searchResultsPage";
    }
}
