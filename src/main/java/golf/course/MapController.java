package golf.course;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/map")
@RequiredArgsConstructor
public class MapController {

    private final Courses courses;

    @Value("${golf.google-maps.api-key:}")
    private String googleMapsApiKey;

    @GetMapping
    public String map(Model model) {
        List<MapPage.MapPoint> mapPoints = courses.getAllCourses().stream()
                .filter(course -> course.lat() != null && course.lng() != null)
                .map(course -> new MapPage.MapPoint(
                        course.name(),
                        course.lat(),
                        course.lng(),
                        "/courses/" + Courses.toCourseSlug(course.name())
                ))
                .toList();

        model.addAttribute("mapPoints", mapPoints);
        model.addAttribute("googleMapsApiKey", googleMapsApiKey);
        return "mapPage";
    }
}
