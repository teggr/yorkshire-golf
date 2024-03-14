package golf.tracker;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class YorkshireChallenge {

    public final Courses courses;
    private final List<CourseRecord> courseRecords = new ArrayList<>();

    @PostConstruct
    public void onLoad() throws IOException {

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new ClassPathResource("tracker.csv").getInputStream(), StandardCharsets.UTF_8.newDecoder()))) {
            boolean ignoreNextLine = true;
            for (; ; ) {
                String line = bufferedReader.readLine();
                if (line == null)
                    break;
                if (ignoreNextLine) {
                    ignoreNextLine = false;
                    continue;
                }
                String[] split = line.split(",");

                Course course = courses.getCourseByName(split[0]);

                courseRecords.add(new CourseRecord(course, LocalDate.parse(split[1])));

            }
        }

    }

    public CourseTracker getTracker() {
        return new CourseTracker(courses.getAllCoursesByRegion(), courseRecords);
    }

}
