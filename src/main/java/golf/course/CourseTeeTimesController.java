package golf.course;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseTeeTimesController {

	private final Courses courses;

	@GetMapping("/{slug}/tee-times")
	public String teeTimes(@PathVariable String slug) {
		Course course = courses.getCourseBySlug(slug);
		return redirectToGolfNow(course.golfnowUrl());
	}

	private String redirectToGolfNow(@Nullable String golfnowUrl) {
		if (golfnowUrl == null || golfnowUrl.isBlank()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tee times not available for this course");
		}

		return "redirect:" + golfnowUrl;
	}
}