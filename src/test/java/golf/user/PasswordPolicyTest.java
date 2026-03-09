package golf.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTest {

    @Test
    void validAlphanumericPasswordPassesValidation() {
        assertThat(PasswordPolicy.validate("abc12345")).isEmpty();
    }

    @Test
    void validPasswordWithUpperAndLowerCasePassesValidation() {
        assertThat(PasswordPolicy.validate("GolfClub99")).isEmpty();
    }

    @Test
    void exactlyEightCharactersPassesValidation() {
        assertThat(PasswordPolicy.validate("abcd1234")).isEmpty();
    }

    @Test
    void sevenCharacterPasswordFailsValidation() {
        Optional<String> error = PasswordPolicy.validate("abc1234");
        assertThat(error).isPresent();
        assertThat(error.get()).containsIgnoringCase("8");
    }

    @Test
    void emptyPasswordFailsValidation() {
        assertThat(PasswordPolicy.validate("")).isPresent();
    }

    @ParameterizedTest
    @ValueSource(strings = {"password!", "pass@123", "p#ssword", "pass-123", "pass.123"})
    void passwordWithNonAlphanumericCharacterFailsValidation(String password) {
        Optional<String> error = PasswordPolicy.validate(password);
        assertThat(error).isPresent();
        assertThat(error.get()).containsIgnoringCase("letters and numbers");
    }

    @Test
    void passwordWithSpaceFailsValidation() {
        Optional<String> error = PasswordPolicy.validate("pass word1");
        assertThat(error).isPresent();
    }

}
