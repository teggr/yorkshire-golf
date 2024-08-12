package golf;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import golf.tracker.CourseRecords;
import golf.tracker.Courses;
import golf.tracker.RegionChallengeTracker;
import golf.tracker.YorkshireChallenge;
import golf.web.HomePage;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler for requests to Lambda function via API Gateway
 */
public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "text/html");
        headers.put("X-Custom-Header", "text/html");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);

        try {

            // initialised things
            Courses courses = new Courses();
            courses.onLoad();
            CourseRecords courseRecords = new CourseRecords(courses);
            courseRecords.onLoad();
            YorkshireChallenge yorkshireChallenge = new YorkshireChallenge(
                    courses,
                    courseRecords
            );

            HomePage homePage = new HomePage();

            RegionChallengeTracker tracker = yorkshireChallenge.getTracker();

            String output = homePage.getOutput(Map.of("tracker", tracker));

            return response
                    .withStatusCode(200)
                    .withBody(output);

        } catch (Exception e) {
            return response
                    .withBody("{}")
                    .withStatusCode(500);
        }
    }

}
