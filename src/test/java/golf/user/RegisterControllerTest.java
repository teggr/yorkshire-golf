package golf.user;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegisterControllerTest {
    private final UserService userService = mock(UserService.class);

    @Test
    void localRegistrationIsDisabledByDefault() {
        var controller = new RegisterController(userService, new LocalRegistration(false, "http://localhost:8080"));
        assertThat(controller.register("new@example.com", "GolfClub99", "GolfClub99", new ExtendedModelMap()))
                .isEqualTo("redirect:/register?providerRequired=true");
        verifyNoInteractions(userService);
    }

    @Test
    void localRegistrationCreatesEmailPasswordAccount() {
        var controller = new RegisterController(userService, new LocalRegistration(true, "http://localhost:8080"));
        when(userService.emailExists("new@example.com")).thenReturn(false);

        assertThat(controller.register("new@example.com", "GolfClub99", "GolfClub99", new ExtendedModelMap()))
                .isEqualTo("redirect:/login?registered=true");
        verify(userService).register("new@example.com", "GolfClub99");
    }

    @Test
    void localRegistrationValidatesPasswordConfirmationAndPolicy() {
        var controller = new RegisterController(userService, new LocalRegistration(true, "http://localhost:8080"));
        var model = new ExtendedModelMap();

        assertThat(controller.register("new@example.com", "GolfClub99", "does-not-match", model))
                .isEqualTo("registerPage");
        assertThat(model.getAttribute("error")).isEqualTo("Passwords do not match.");
        verifyNoInteractions(userService);
    }

    @Test
    void localRegistrationHandlesDuplicateEmails() {
        var controller = new RegisterController(userService, new LocalRegistration(true, "http://localhost:8080"));
        when(userService.emailExists("new@example.com")).thenReturn(true);
        var model = new ExtendedModelMap();

        assertThat(controller.register("new@example.com", "GolfClub99", "GolfClub99", model))
                .isEqualTo("registerPage");
        assertThat(model.getAttribute("error")).isEqualTo("An account with that email address already exists.");
    }

    @Test
    void localRegistrationRejectsNonLoopbackOrigin() {
        assertThatThrownBy(() -> new LocalRegistration(true, "https://yorkshiregolf.life"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("APP_BASE_URL");
    }
}
