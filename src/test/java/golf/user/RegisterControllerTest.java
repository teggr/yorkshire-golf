package golf.user;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisterControllerTest {

    @Test
    void duplicateEmailShowsErrorAndDoesNotRegister() {
        UserService userService = mock(UserService.class);
        RegisterController controller = new RegisterController(userService);
        Model model = new ExtendedModelMap();

        when(userService.emailExists("user@example.com")).thenReturn(true);

        String view = controller.register("user@example.com", "GolfClub99", "GolfClub99", model);

        assertThat(view).isEqualTo("registerPage");
        assertThat(model.getAttribute("error")).isEqualTo("An account with that email address already exists.");
        verify(userService, never()).register("user@example.com", "GolfClub99");
    }

    @Test
    void duplicateEmailRaisedDuringRegisterShowsSameError() {
        UserService userService = mock(UserService.class);
        RegisterController controller = new RegisterController(userService);
        Model model = new ExtendedModelMap();

        when(userService.emailExists("user@example.com")).thenReturn(false);
        org.mockito.Mockito.when(userService.register("user@example.com", "GolfClub99"))
                .thenThrow(new DuplicateEmailException("user@example.com"));

        String view = controller.register("user@example.com", "GolfClub99", "GolfClub99", model);

        assertThat(view).isEqualTo("registerPage");
        assertThat(model.getAttribute("error")).isEqualTo("An account with that email address already exists.");
    }
}