package golf.tracker;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class Courses {

    private final List<Course> courses = new ArrayList<>();

    @PostConstruct
    public void onLoad() throws IOException {

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new ClassPathResource("courses.csv").getInputStream(), StandardCharsets.UTF_8.newDecoder()))) {
            boolean ignoreNextLine = true;
            for (; ; ) {
                if (ignoreNextLine) {
                    ignoreNextLine = false;
                    continue;
                }
                String line = bufferedReader.readLine();
                if (line == null)
                    break;
                String[] split = line.split(",");
                courses.add(new Course(split[0], new Region(split[1])));
            }
        }

    }

    public List<Course> getAllCourses() {
        return new ArrayList<>(courses);
    }

}
