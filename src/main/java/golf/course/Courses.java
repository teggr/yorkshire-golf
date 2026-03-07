package golf.course;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class Courses {

    private final List<Course> courses = new ArrayList<>();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @PostConstruct
    public void onLoad() throws IOException {

        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:courses/*.yaml");
        
        for (Resource resource : resources) {
            Course course = yamlMapper.readValue(resource.getInputStream(), Course.class);
            courses.add(course);
        }
        
        log.info("Loaded {} courses from YAML files", courses.size());

    }

    public List<Course> getAllCourses() {
        return courses.stream()
                .filter(course -> !course.closed())
                .toList();
    }

    public Course getCourseByName(String name) {
        return courses.stream().filter(c -> c.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find " + name));
    }

    public List<Course> getPlayAndStayCourses() {
        return courses.stream()
                .filter(course -> !course.closed())
                .filter(Course::playAndStay)
                .toList();
    }

    public List<Course> getTop100Courses() {
        return courses.stream()
                .filter(course -> !course.closed())
                .filter(course -> course.top100() != null)
                .sorted(java.util.Comparator.comparingInt(Course::top100))
                .toList();
    }

    public Map<Region, Long> getCourseRegionCountGroupByRegion() {
        return courses.stream()
                .filter(course -> !course.closed())
                .collect(
                        Collectors.groupingBy(
                                Course::region,
                                Collectors.counting()
                        )
                );
    }
}
