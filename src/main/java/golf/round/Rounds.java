package golf.round;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class Rounds {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final List<Round> rounds = new ArrayList<>();

    @PostConstruct
    @SneakyThrows
    public void onLoad() {
        Round[] loaded = objectMapper.readValue(
                new ClassPathResource("round-records.json").getInputStream(),
                Round[].class
        );
        rounds.addAll(Arrays.asList(loaded));
        rounds.sort((a, b) -> b.date().compareTo(a.date()));
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

}
