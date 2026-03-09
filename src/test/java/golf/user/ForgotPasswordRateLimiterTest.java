package golf.user;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class ForgotPasswordRateLimiterTest {

    @Test
    void allowsThreeRequestsPerHourAndBlocksFourth() {
        MutableClock clock = new MutableClock(Instant.parse("2026-03-09T10:00:00Z"));
        ForgotPasswordRateLimiter limiter = new ForgotPasswordRateLimiter(clock);

        assertThat(limiter.allow("test@example.com")).isTrue();
        assertThat(limiter.allow("test@example.com")).isTrue();
        assertThat(limiter.allow("test@example.com")).isTrue();
        assertThat(limiter.allow("test@example.com")).isFalse();
    }

    @Test
    void windowExpiresAfterOneHour() {
        MutableClock clock = new MutableClock(Instant.parse("2026-03-09T10:00:00Z"));
        ForgotPasswordRateLimiter limiter = new ForgotPasswordRateLimiter(clock);

        limiter.allow("test@example.com");
        limiter.allow("test@example.com");
        limiter.allow("test@example.com");

        clock.advance(Duration.ofHours(1).plusSeconds(1));

        assertThat(limiter.allow("test@example.com")).isTrue();
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
