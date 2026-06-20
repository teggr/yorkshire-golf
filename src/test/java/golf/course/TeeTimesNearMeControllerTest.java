package golf.course;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeeTimesNearMeControllerTest {

    @Test
    void teeTimesNearMeRedirectsToGolfNowCoursesNearMe() {
        TeeTimesNearMeController controller = new TeeTimesNearMeController();

        String view = controller.teeTimesNearMe();

        assertEquals("redirect:https://www.golfnow.co.uk/tee-times/courses-near-me", view);
    }
}