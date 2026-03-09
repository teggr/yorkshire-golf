package golf.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    @Test
    void locksAfterFiveFailuresAndUnlocksAfterDuration() {
        MutableClock clock = new MutableClock(Instant.parse("2026-03-09T10:00:00Z"));
        LoginAttemptService service = new LoginAttemptService(clock);

        for (int i = 0; i < 5; i++) {
            service.recordFailure("test@example.com");
        }

        assertThat(service.isLocked("test@example.com")).isTrue();

        clock.advance(Duration.ofMinutes(16));

        assertThat(service.isLocked("test@example.com")).isFalse();
    }

    @Test
    void successfulLoginResetsFailures() {
        MutableClock clock = new MutableClock(Instant.parse("2026-03-09T10:00:00Z"));
        LoginAttemptService service = new LoginAttemptService(clock);

        for (int i = 0; i < 4; i++) {
            service.recordFailure("test@example.com");
        }
        service.recordSuccess("test@example.com");
        service.recordFailure("test@example.com");

        assertThat(service.isLocked("test@example.com")).isFalse();
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
