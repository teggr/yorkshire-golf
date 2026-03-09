package golf.user;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ForgotPasswordControllerTest {

    @Test
    void sendsResetEmailWhenRateLimiterAllows() {
        UserService userService = mock(UserService.class);
        ForgotPasswordRateLimiter limiter = mock(ForgotPasswordRateLimiter.class);
        ForgotPasswordController controller = new ForgotPasswordController(userService, limiter);
        ReflectionTestUtils.setField(controller, "appBaseUrl", "https://example.com");

        Model model = new ExtendedModelMap();

        org.mockito.Mockito.when(limiter.allow("user@example.com")).thenReturn(true);

        String view = controller.handleForgotPassword("email", "user@example.com", null, null, null, model);

        assertThat(view).isEqualTo("forgotPasswordPage");
        assertThat(model.getAttribute("step")).isEqualTo("sent");
        verify(userService).sendPasswordResetEmail("user@example.com", "https://example.com/forgot-password");
    }

    @Test
    void doesNotSendResetEmailWhenRateLimiterBlocks() {
        UserService userService = mock(UserService.class);
        ForgotPasswordRateLimiter limiter = mock(ForgotPasswordRateLimiter.class);
        ForgotPasswordController controller = new ForgotPasswordController(userService, limiter);
        ReflectionTestUtils.setField(controller, "appBaseUrl", "https://example.com");

        Model model = new ExtendedModelMap();

        org.mockito.Mockito.when(limiter.allow("user@example.com")).thenReturn(false);

        String view = controller.handleForgotPassword("email", "user@example.com", null, null, null, model);

        assertThat(view).isEqualTo("forgotPasswordPage");
        assertThat(model.getAttribute("step")).isEqualTo("sent");
        verify(userService, never()).sendPasswordResetEmail(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }
}
