package golf.course;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class GeocodingService {

    private static final long RATE_LIMIT_DELAY_MS = 1100;

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeocodingService(
            @Value("${geocoding.user-agent:YorkshireGolf/1.0 (https://yorkshiregolf.life)}") String userAgent) {
        this.restClient = RestClient.builder()
                .defaultHeader("User-Agent", userAgent)
                .build();
    }

    /**
     * Geocodes a course by its address using Nominatim.
     * Returns a double array [lat, lng], or null if geocoding failed or address is missing.
     */
    public double[] geocode(Course course) {
        if (course.address() == null || course.address().isBlank()) {
            log.warn("Cannot geocode course '{}': no address", course.name());
            return null;
        }

        try {
            Thread.sleep(RATE_LIMIT_DELAY_MS);

            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("nominatim.openstreetmap.org")
                            .path("/search")
                            .queryParam("q", course.address())
                            .queryParam("format", "json")
                            .queryParam("limit", "1")
                            .build())
                    .retrieve()
                    .body(String.class);

            if (response == null || response.isBlank()) {
                log.warn("Nominatim returned empty response for course '{}'", course.name());
                return null;
            }

            JsonNode results = objectMapper.readTree(response);
            if (!results.isArray() || results.isEmpty()) {
                log.warn("Nominatim returned no results for course '{}' with address '{}'",
                        course.name(), course.address());
                return null;
            }

            JsonNode first = results.get(0);
            double lat = first.get("lat").asDouble();
            double lng = first.get("lon").asDouble();
            log.info("Geocoded '{}': lat={}, lng={}", course.name(), lat, lng);
            return new double[]{lat, lng};

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Geocoding interrupted for course '{}'", course.name());
            return null;
        } catch (Exception e) {
            log.warn("Geocoding failed for course '{}': {}", course.name(), e.getMessage());
            return null;
        }
    }
}
