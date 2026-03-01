package golf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility to add placeholder mainImageUrl to all courses that don't have one.
 * Run as a main class after scraping images.
 */
@Slf4j
public class AddPlaceholderImages {

    private static final String COURSES_YAML_DIR = "src/main/resources/courses";
    private static final String PLACEHOLDER_IMAGE_URL = "/images/courses/placeholder-course.jpg";

    public static void main(String[] args) throws Exception {

        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));

        Path coursesDir = Path.of(COURSES_YAML_DIR);
        int updated = 0;

        try (var stream = Files.list(coursesDir)) {
            for (Path yamlFile : (Iterable<Path>) stream.sorted()::iterator) {
                if (!yamlFile.toString().endsWith(".yaml")) continue;

                @SuppressWarnings("unchecked")
                Map<String, Object> courseData = yamlMapper.readValue(yamlFile.toFile(), Map.class);

                // Skip if already has mainImageUrl
                if (courseData.containsKey("mainImageUrl")) {
                    continue;
                }

                log.info("Adding placeholder to: {}", yamlFile.getFileName());

                // Add placeholder
                Map<String, Object> updatedData = new LinkedHashMap<>(courseData);
                updatedData.put("mainImageUrl", PLACEHOLDER_IMAGE_URL);

                yamlMapper.writeValue(yamlFile.toFile(), updatedData);
                updated++;
            }
        }

        log.info("Done. Added placeholder to {} courses.", updated);
    }
}
