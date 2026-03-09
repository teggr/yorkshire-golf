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
            @RequestParam(required = false) String actionError,
            @RequestParam(required = false) String imported,
            @RequestParam(required = false) String deleted,
            @RequestParam(required = false) String updated,
            Model model
    ) {
        GolfUser trackerOwner = userService.findByTrackerId(trackerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tracker not found"));

        GolfUser currentUser = resolveUser(userDetails);
        boolean canAddRound = currentUser != null && currentUser.id().equals(trackerOwner.id());

        RegionChallengeTracker tracker = yorkshireChallenge.getTrackerForUser(trackerOwner.id());
        MonthlyCourseProgress monthlyProgress = yorkshireChallenge.getMonthlyCourseProgressForUser(trackerOwner.id());

        model.addAttribute("tracker", tracker);
        model.addAttribute("trackerId", trackerId);
        model.addAttribute("canAddRound", canAddRound);
        model.addAttribute("monthlyProgressLabels", monthlyProgress.labels());
        model.addAttribute("monthlyProgressValues", monthlyProgress.cumulativeCoursesPlayed());
        model.addAttribute("monthlyProgressMax", tracker.totalCourseCount());

        if (canAddRound) {
            model.addAttribute("allCourses", courses.getAllCourses());
        }

        if ("true".equals(added)) {
            model.addAttribute("success", "Round added successfully.");
        }

        if (imported != null) {
            model.addAttribute("success", importSuccessMessage(imported));
        }

        if ("true".equals(deleted)) {
            model.addAttribute("success", "Round deleted successfully.");
        }

        if ("true".equals(updated)) {
            model.addAttribute("success", "Round date updated successfully.");
        }

        if ("duplicate".equals(error)) {
            model.addAttribute("error", "You have already logged a round at that course.");
        }

        if ("not-found".equals(actionError)) {
            model.addAttribute("error", "Round not found.");
        }

        if ("invalid-date".equals(actionError)) {
            model.addAttribute("error", "Please provide a valid date.");
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

    @PostMapping("/{trackerId}/delete-round")
    public String deleteRound(
            @PathVariable String trackerId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String roundId,
            RedirectAttributes redirectAttributes
    ) {
        GolfUser trackerOwner = userService.findByTrackerId(trackerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tracker not found"));

        GolfUser currentUser = resolveUser(userDetails);
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (!currentUser.id().equals(trackerOwner.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the tracker owner can delete rounds");
        }

        Long parsedRoundId = parseRoundId(roundId);
        int deleted = userRoundRepository.deleteByIdAndUserId(parsedRoundId, currentUser.id());
        if (deleted == 0) {
            redirectAttributes.addAttribute("actionError", "not-found");
            return "redirect:/challenge/" + trackerId;
        }

        redirectAttributes.addAttribute("deleted", "true");
        return "redirect:/challenge/" + trackerId;
    }

    @PostMapping("/{trackerId}/update-round-date")
    public String updateRoundDate(
            @PathVariable String trackerId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String roundId,
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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the tracker owner can update rounds");
        }

        if (date == null || date.isBlank()) {
            redirectAttributes.addAttribute("actionError", "invalid-date");
            return "redirect:/challenge/" + trackerId;
        }

        Long parsedRoundId = parseRoundId(roundId);
        int updated = userRoundRepository.updateDate(parsedRoundId, currentUser.id(), date);
        if (updated == 0) {
            redirectAttributes.addAttribute("actionError", "not-found");
            return "redirect:/challenge/" + trackerId;
        }

        redirectAttributes.addAttribute("updated", "true");
        return "redirect:/challenge/" + trackerId;
    }

    private Long parseRoundId(String roundId) {
        try {
            return Long.valueOf(roundId);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid round id");
        }
    }

    private String importSuccessMessage(String imported) {
        try {
            int count = Integer.parseInt(imported);
            return count == 1 ? "1 round imported successfully." : count + " rounds imported successfully.";
        } catch (NumberFormatException ex) {
            return imported + " rounds imported successfully.";
        }
    }

    private GolfUser resolveUser(UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        return userService.findByEmail(userDetails.getUsername()).orElse(null);
    }

}
