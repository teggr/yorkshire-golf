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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Locale;
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

    public List<Course> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        List<String> tokens = tokenize(query);
        if (tokens.isEmpty()) {
            return List.of();
        }

        return courses.stream()
                .filter(course -> !course.closed())
                .map(course -> new SearchHit(course, countTokenMatches(course, tokens)))
                .filter(hit -> hit.matchCount > 0)
                .sorted(Comparator
                        .comparingInt(SearchHit::matchCount).reversed()
                        .thenComparing(hit -> hit.course.name(), String.CASE_INSENSITIVE_ORDER))
                .map(SearchHit::course)
                .toList();
    }

    private static int countTokenMatches(Course course, List<String> tokens) {
        String searchable = ((course.name() == null ? "" : course.name()) + " "
                + (course.address() == null ? "" : course.address()))
                .toLowerCase(Locale.ROOT);

        int matches = 0;
        for (String token : tokens) {
            if (searchable.contains(token)) {
                matches++;
            }
        }

        return matches;
    }

    private static List<String> tokenize(String query) {
        return java.util.Arrays.stream(query.toLowerCase(Locale.ROOT).trim().split("\\s+"))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .distinct()
                .toList();
    }

    private record SearchHit(Course course, int matchCount) {
    }

    public Course getCourseByName(String name) {
        return courses.stream().filter(c -> c.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find " + name));
    }

    public Course getCourseBySlug(String slug) {
        return courses.stream()
                .filter(c -> toCourseSlug(c.name()).equals(slug))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find course with slug: " + slug));
    }

    public static String toCourseSlug(String courseName) {
        return courseName.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");
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
  
    public List<Course> getNext100Courses() {
        return courses.stream()
                .filter(course -> !course.closed())
                .filter(course -> Boolean.TRUE.equals(course.next100()))
                .sorted(java.util.Comparator.comparing(Course::name, String.CASE_INSENSITIVE_ORDER))
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
