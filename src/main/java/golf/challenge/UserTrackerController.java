package golf.challenge;

import golf.course.Courses;
import golf.user.GolfUser;
import golf.user.UserRoundRepository;
import golf.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/challenge")
@RequiredArgsConstructor
public class UserTrackerController {

    private final UserService userService;
    private final YorkshireChallenge yorkshireChallenge;
    private final Courses courses;
    private final UserRoundRepository userRoundRepository;

    @GetMapping("/{trackerId}")
    public String tracker(
            @PathVariable String trackerId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String added,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String imported,
            Model model
    ) {
        GolfUser trackerOwner = userService.findByTrackerId(trackerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tracker not found"));

        GolfUser currentUser = resolveUser(userDetails);
        boolean canAddRound = currentUser != null && currentUser.id().equals(trackerOwner.id());

        model.addAttribute("tracker", yorkshireChallenge.getTrackerForUser(trackerOwner.id()));
        model.addAttribute("trackerId", trackerId);
        model.addAttribute("canAddRound", canAddRound);

        if (canAddRound) {
            model.addAttribute("allCourses", courses.getAllCourses());
        }

        if ("true".equals(added)) {
            model.addAttribute("success", "Round added.");
        }

        if (imported != null) {
            model.addAttribute("success", imported + " round(s) imported.");
        }

        if ("duplicate".equals(error)) {
            model.addAttribute("error", "You have already logged a round at that course.");
        }

        return "regionTrackerPage";
    }

    @PostMapping("/{trackerId}/add-round")
    public String addRound(
            @PathVariable String trackerId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String courseName,
            @RequestParam String date,
            RedirectAttributes redirectAttributes
    ) {
        GolfUser trackerOwner = userService.findByTrackerId(trackerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tracker not found"));

        GolfUser currentUser = resolveUser(userDetails);
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (!currentUser.id().equals(trackerOwner.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the tracker owner can add rounds");
        }

        if (userRoundRepository.existsByUserIdAndCourseName(currentUser.id(), courseName)) {
            redirectAttributes.addAttribute("error", "duplicate");
            return "redirect:/challenge/" + trackerId;
        }

        userRoundRepository.save(currentUser.id(), courseName, date);
        redirectAttributes.addAttribute("added", "true");
        return "redirect:/challenge/" + trackerId;
    }

    @PostMapping("/{trackerId}/import-rounds")
    public String importRounds(
            @PathVariable String trackerId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("csvFile") MultipartFile csvFile,
            RedirectAttributes redirectAttributes
    ) throws Exception {
        GolfUser trackerOwner = userService.findByTrackerId(trackerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tracker not found"));

        GolfUser currentUser = resolveUser(userDetails);
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (!currentUser.id().equals(trackerOwner.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the tracker owner can import rounds");
        }

        int imported = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvFile.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; }
                String[] parts = line.split(",", 2);
                if (parts.length < 2) continue;
                String courseName = parts[0].trim();
                String date = parts[1].trim();
                if (courseName.isBlank() || date.isBlank()) continue;
                if (!userRoundRepository.existsByUserIdAndCourseName(currentUser.id(), courseName)) {
                    userRoundRepository.save(currentUser.id(), courseName, date);
                    imported++;
                }
            }
        }

        redirectAttributes.addAttribute("imported", imported);
        return "redirect:/challenge/" + trackerId;
    }

    private GolfUser resolveUser(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        return userService.findByEmail(userDetails.getUsername()).orElse(null);
    }

}
