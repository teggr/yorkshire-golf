package golf.tracker;

import lombok.RequiredArgsConstructor;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class CourseRecords {

    public final Courses courses;
    private final List<CourseRecord> courseRecords = new ArrayList<>();

    public void onLoad() throws IOException {

        try (BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(new FileInputStream("course-records.csv"), StandardCharsets.UTF_8.newDecoder())
                )) {
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

    public List<CourseRecord> getCourseRecordsWhereRegionIn(Region... regions) {

        return courseRecords.stream()
                .filter(cr -> Stream.of(regions).anyMatch(r -> r.equals(cr.course().region())))
                .collect(Collectors.toList());

    }

}
