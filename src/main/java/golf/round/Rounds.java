package golf.round;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import golf.course.Courses;
import golf.course.Region;
import golf.user.UserRound;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class Rounds {

    private final Courses courses;
    private final List<UserRound> rounds = new ArrayList<>();

    @PostConstruct
    @SneakyThrows
    public void onLoad() {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:rounds/*.yaml");

        for (Resource resource : resources) {
            RoundYaml loaded = yamlMapper.readValue(resource.getInputStream(), RoundYaml.class);
            var course = courses.getCourseByName(loaded.courseName());
            rounds.add(new UserRound(
                    loaded.id(),
                    null,
                    loaded.title(),
                    loaded.date(),
                    loaded.courseName(),
                    course,
                    loaded.imageUrls(),
                    loaded.content()
            ));
        }

        rounds.sort((a, b) -> b.date().compareTo(a.date()));
        log.info("Loaded {} rounds from YAML files", rounds.size());
    }

    public List<UserRound> getAllRounds() {
        return new ArrayList<>(rounds);
    }

    public UserRound getRoundById(String id) {
        return rounds.stream()
                .filter(r -> r.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Round not found: " + id));
    }

    public List<UserRound> getRoundsWhereRegionIn(Region... regions) {
        Set<Region> regionSet = Set.of(regions);
        return rounds.stream()
                .filter(r -> hasCourseInRegions(r, regionSet))
                .toList();
    }

    private static boolean hasCourseInRegions(UserRound round, Set<Region> regions) {
        return round.course() != null && regions.contains(round.course().region());
    }

}
