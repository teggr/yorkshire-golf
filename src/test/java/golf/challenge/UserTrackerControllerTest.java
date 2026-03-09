package golf.challenge;

import golf.course.Course;
import golf.course.Courses;
import golf.user.GolfUser;
import golf.user.UserRoundRepository;
import golf.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserTrackerControllerTest {

    @Test
    void trackerAddsTrackerModelAndReturnsTrackerViewForAnonymousUser() {
        UserService userService = mock(UserService.class);
        YorkshireChallenge yorkshireChallenge = mock(YorkshireChallenge.class);
    Courses courses = mock(Courses.class);
    UserRoundRepository userRoundRepository = mock(UserRoundRepository.class);
    RegionChallengeTracker tracker = mock(RegionChallengeTracker.class);
    UserTrackerController controller = new UserTrackerController(userService, yorkshireChallenge, courses, userRoundRepository);
        Model model = new ExtendedModelMap();

        when(userService.findByTrackerId("abc123tracker"))
                .thenReturn(Optional.of(new GolfUser(10L, "g@example.com", "secret", "abc123tracker", "USER", false)));
        when(yorkshireChallenge.getTrackerForUser(10L)).thenReturn(tracker);

    String view = controller.tracker("abc123tracker", null, null, null, null, model);

        assertEquals("regionTrackerPage", view);
        assertEquals(tracker, model.getAttribute("tracker"));
    assertEquals("abc123tracker", model.getAttribute("trackerId"));
    assertFalse((Boolean) model.getAttribute("canAddRound"));
    }

    @Test
    void trackerIncludesAddRoundModelForTrackerOwner() {
    UserService userService = mock(UserService.class);
    YorkshireChallenge yorkshireChallenge = mock(YorkshireChallenge.class);
    Courses courses = mock(Courses.class);
    UserRoundRepository userRoundRepository = mock(UserRoundRepository.class);
    RegionChallengeTracker tracker = mock(RegionChallengeTracker.class);
    UserTrackerController controller = new UserTrackerController(userService, yorkshireChallenge, courses, userRoundRepository);
    Model model = new ExtendedModelMap();

    UserDetails userDetails = User.withUsername("g@example.com")
        .password("unused")
        .roles("USER")
        .build();

    Course course = mock(Course.class);

    when(userService.findByTrackerId("abc123tracker"))
        .thenReturn(Optional.of(new GolfUser(10L, "g@example.com", "secret", "abc123tracker", "USER", false)));
    when(userService.findByEmail("g@example.com"))
        .thenReturn(Optional.of(new GolfUser(10L, "g@example.com", "secret", "abc123tracker", "USER", false)));
    when(yorkshireChallenge.getTrackerForUser(10L)).thenReturn(tracker);
    when(courses.getAllCourses()).thenReturn(List.of(course));

    String view = controller.tracker("abc123tracker", userDetails, "true", null, null, model);

    assertEquals("regionTrackerPage", view);
    assertTrue((Boolean) model.getAttribute("canAddRound"));
    assertEquals(List.of(course), model.getAttribute("allCourses"));
    assertEquals("Round added.", model.getAttribute("success"));
    }

    @Test
    void addRoundSavesForTrackerOwnerAndRedirectsToTrackerPage() {
    UserService userService = mock(UserService.class);
    YorkshireChallenge yorkshireChallenge = mock(YorkshireChallenge.class);
    Courses courses = mock(Courses.class);
    UserRoundRepository userRoundRepository = mock(UserRoundRepository.class);
    UserTrackerController controller = new UserTrackerController(userService, yorkshireChallenge, courses, userRoundRepository);

    UserDetails userDetails = User.withUsername("g@example.com")
        .password("unused")
        .roles("USER")
        .build();

    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    GolfUser owner = new GolfUser(10L, "g@example.com", "secret", "abc123tracker", "USER", false);
    when(userService.findByTrackerId("abc123tracker")).thenReturn(Optional.of(owner));
    when(userService.findByEmail("g@example.com")).thenReturn(Optional.of(owner));
    when(userRoundRepository.existsByUserIdAndCourseName(10L, "Alwoodley")).thenReturn(false);

    String view = controller.addRound("abc123tracker", userDetails, "Alwoodley", "2026-03-07", redirectAttributes);

    assertEquals("redirect:/challenge/abc123tracker", view);
    assertEquals("true", redirectAttributes.getAttribute("added"));
    verify(userRoundRepository).save(10L, "Alwoodley", "2026-03-07");
    }

    @Test
    void addRoundRejectsLoggedInNonOwner() {
    UserService userService = mock(UserService.class);
    YorkshireChallenge yorkshireChallenge = mock(YorkshireChallenge.class);
    Courses courses = mock(Courses.class);
    UserRoundRepository userRoundRepository = mock(UserRoundRepository.class);
    UserTrackerController controller = new UserTrackerController(userService, yorkshireChallenge, courses, userRoundRepository);

    UserDetails userDetails = User.withUsername("other@example.com")
        .password("unused")
        .roles("USER")
        .build();

    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(userService.findByTrackerId("abc123tracker"))
        .thenReturn(Optional.of(new GolfUser(10L, "owner@example.com", "secret", "abc123tracker", "USER", false)));
    when(userService.findByEmail("other@example.com"))
        .thenReturn(Optional.of(new GolfUser(20L, "other@example.com", "secret", "zzz", "USER", false)));

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> controller.addRound("abc123tracker", userDetails, "Alwoodley", "2026-03-07", redirectAttributes));

    assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    verify(userRoundRepository, never()).save(10L, "Alwoodley", "2026-03-07");
    }

    @Test
    void trackerThrowsNotFoundWhenTrackerIdMissing() {
        UserService userService = mock(UserService.class);
        YorkshireChallenge yorkshireChallenge = mock(YorkshireChallenge.class);
    Courses courses = mock(Courses.class);
    UserRoundRepository userRoundRepository = mock(UserRoundRepository.class);
    UserTrackerController controller = new UserTrackerController(userService, yorkshireChallenge, courses, userRoundRepository);
        Model model = new ExtendedModelMap();

        when(userService.findByTrackerId("unknown")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> controller.tracker("unknown", null, null, null, null, model));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void importRoundsSavesNewRoundsAndRedirects() throws Exception {
        UserService userService = mock(UserService.class);
        YorkshireChallenge yorkshireChallenge = mock(YorkshireChallenge.class);
        Courses courses = mock(Courses.class);
        UserRoundRepository userRoundRepository = mock(UserRoundRepository.class);
        UserTrackerController controller = new UserTrackerController(userService, yorkshireChallenge, courses, userRoundRepository);

        UserDetails userDetails = User.withUsername("g@example.com").password("unused").roles("USER").build();
        GolfUser owner = new GolfUser(10L, "g@example.com", "secret", "abc123tracker", "USER", false);
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        when(userService.findByTrackerId("abc123tracker")).thenReturn(Optional.of(owner));
        when(userService.findByEmail("g@example.com")).thenReturn(Optional.of(owner));
        when(userRoundRepository.existsByUserIdAndCourseName(10L, "Alwoodley Golf Club")).thenReturn(false);
        when(userRoundRepository.existsByUserIdAndCourseName(10L, "Moortown Golf Club")).thenReturn(false);

        String csv = "courseName,date\nAlwoodley Golf Club,2024-05-24\nMoortown Golf Club,2023-04-09\n";
        MockMultipartFile file = new MockMultipartFile("csvFile", "rounds.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        String view = controller.importRounds("abc123tracker", userDetails, file, redirectAttributes);

        assertEquals("redirect:/challenge/abc123tracker", view);
        assertEquals("2", String.valueOf(redirectAttributes.getAttribute("imported")));
        verify(userRoundRepository).save(10L, "Alwoodley Golf Club", "2024-05-24");
        verify(userRoundRepository).save(10L, "Moortown Golf Club", "2023-04-09");
    }

    @Test
    void importRoundsSkipsDuplicateSilently() throws Exception {
        UserService userService = mock(UserService.class);
        YorkshireChallenge yorkshireChallenge = mock(YorkshireChallenge.class);
        Courses courses = mock(Courses.class);
        UserRoundRepository userRoundRepository = mock(UserRoundRepository.class);
        UserTrackerController controller = new UserTrackerController(userService, yorkshireChallenge, courses, userRoundRepository);

        UserDetails userDetails = User.withUsername("g@example.com").password("unused").roles("USER").build();
        GolfUser owner = new GolfUser(10L, "g@example.com", "secret", "abc123tracker", "USER", false);
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        when(userService.findByTrackerId("abc123tracker")).thenReturn(Optional.of(owner));
        when(userService.findByEmail("g@example.com")).thenReturn(Optional.of(owner));
        when(userRoundRepository.existsByUserIdAndCourseName(10L, "Alwoodley Golf Club")).thenReturn(true);
        when(userRoundRepository.existsByUserIdAndCourseName(10L, "Moortown Golf Club")).thenReturn(false);

        String csv = "courseName,date\nAlwoodley Golf Club,2024-05-24\nMoortown Golf Club,2023-04-09\n";
        MockMultipartFile file = new MockMultipartFile("csvFile", "rounds.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        String view = controller.importRounds("abc123tracker", userDetails, file, redirectAttributes);

        assertEquals("redirect:/challenge/abc123tracker", view);
        assertEquals("1", String.valueOf(redirectAttributes.getAttribute("imported")));
        verify(userRoundRepository, never()).save(10L, "Alwoodley Golf Club", "2024-05-24");
        verify(userRoundRepository).save(10L, "Moortown Golf Club", "2023-04-09");
    }

    @Test
    void importRoundsRejectsNonOwner() throws Exception {
        UserService userService = mock(UserService.class);
        YorkshireChallenge yorkshireChallenge = mock(YorkshireChallenge.class);
        Courses courses = mock(Courses.class);
        UserRoundRepository userRoundRepository = mock(UserRoundRepository.class);
        UserTrackerController controller = new UserTrackerController(userService, yorkshireChallenge, courses, userRoundRepository);

        UserDetails userDetails = User.withUsername("other@example.com").password("unused").roles("USER").build();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        when(userService.findByTrackerId("abc123tracker"))
            .thenReturn(Optional.of(new GolfUser(10L, "owner@example.com", "secret", "abc123tracker", "USER", false)));
        when(userService.findByEmail("other@example.com"))
            .thenReturn(Optional.of(new GolfUser(20L, "other@example.com", "secret", "zzz", "USER", false)));

        String csv = "courseName,date\nAlwoodley Golf Club,2024-05-24\n";
        MockMultipartFile file = new MockMultipartFile("csvFile", "rounds.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.importRounds("abc123tracker", userDetails, file, redirectAttributes));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(userRoundRepository, never()).save(any(), any(), any());
    }
}