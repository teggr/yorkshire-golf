package golf.round;

import golf.course.Courses;
import golf.course.Region;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class Rounds {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Courses courses;
    private final List<Round> rounds = new ArrayList<>();

    @PostConstruct
    @SneakyThrows
    public void onLoad() {
        Round[] loaded = objectMapper.readValue(
                new ClassPathResource("round-records.json").getInputStream(),
                Round[].class
        );
        Arrays.stream(loaded)
                .map(r -> {
                    var course = courses.getCourseByName(r.courseName());
                    return new Round(r.id(), r.title(), r.date(), r.courseName(), course, r.imageUrls(), r.content());
                })
                .sorted((a, b) -> b.date().compareTo(a.date()))
                .forEach(rounds::add);
    }

    public List<Round> getAllRounds() {
        return new ArrayList<>(rounds);
    }

    public Round getRoundById(String id) {
        return rounds.stream()
                .filter(r -> r.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Round not found: " + id));
    }

    public List<Round> getRoundsWhereRegionIn(Region... regions) {
        Set<Region> regionSet = Set.of(regions);
        return rounds.stream()
                .filter(r -> regionSet.contains(r.course().region()))
                .toList();
    }

}
