package golf.tracker;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Courses {

    private final List<Course> courses = new ArrayList<>();

    public void onLoad() throws IOException {

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream("courses.csv"), StandardCharsets.UTF_8.newDecoder()))) {
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
                courses.add(new Course(split[0], new Region(split[1])));
            }
        }

    }

    public List<Course> getAllCourses() {
        return new ArrayList<>(courses);
    }

    public Course getCourseByName(String name) {
        return courses.stream().filter(c -> c.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find " + name));
    }

    public Map<Region, Long> getCourseRegionCountGroupByRegion() {
        return courses.stream().collect(
                Collectors.groupingBy(
                        Course::region,
                        Collectors.counting()
                )
        );
    }
}
