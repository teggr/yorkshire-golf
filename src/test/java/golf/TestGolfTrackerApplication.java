package golf;

import org.springframework.boot.SpringApplication;

/**
 * Local development entry point. Starts the application with a Mailpit SMTP container
 * managed by Testcontainers. Run this class instead of {@link GolfTrackerApplication}
 * during local development to get a live mail sandbox at http://localhost:8025.
 */
public class TestGolfTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.from(GolfTrackerApplication::main)
				.with(TestContainersConfiguration.class)
				.run(args);
	}

}
