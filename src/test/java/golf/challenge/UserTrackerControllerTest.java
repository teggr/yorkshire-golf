package golf.challenge;

import golf.user.GolfUser;
import golf.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserTrackerControllerTest {

    @Test
    void trackerAddsTrackerModelAndReturnsTrackerView() {
        UserService userService = mock(UserService.class);
        YorkshireChallenge yorkshireChallenge = mock(YorkshireChallenge.class);
        RegionChallengeTracker tracker = mock(RegionChallengeTracker.class);
        UserTrackerController controller = new UserTrackerController(userService, yorkshireChallenge);
        Model model = new ExtendedModelMap();

        when(userService.findByTrackerId("abc123tracker"))
                .thenReturn(Optional.of(new GolfUser(10L, "g@example.com", "secret", "abc123tracker", "USER", false)));
        when(yorkshireChallenge.getTrackerForUser(10L)).thenReturn(tracker);

        String view = controller.tracker("abc123tracker", model);

        assertEquals("regionTrackerPage", view);
        assertEquals(tracker, model.getAttribute("tracker"));
    }

    @Test
    void trackerThrowsNotFoundWhenTrackerIdMissing() {
        UserService userService = mock(UserService.class);
        YorkshireChallenge yorkshireChallenge = mock(YorkshireChallenge.class);
        UserTrackerController controller = new UserTrackerController(userService, yorkshireChallenge);
        Model model = new ExtendedModelMap();

        when(userService.findByTrackerId("unknown")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.tracker("unknown", model));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}