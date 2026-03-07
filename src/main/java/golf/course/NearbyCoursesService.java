package golf.course;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class NearbyCoursesService {

    private static final String COURSES_PATH = "src/main/resources/courses";

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    /**
     * For every course that has valid lat/lng, computes the 3 nearest courses using the
     * Haversine formula and writes nearby1/nearby2/nearby3 back to the YAML files.
     *
     * @return a human-readable summary of what was computed
     */
    public String computeAll() throws IOException {
        Path coursesDir = Paths.get(COURSES_PATH);
        List<Path> courseFiles = Files.list(coursesDir)
                .filter(p -> p.toString().endsWith(".yaml"))
                .sorted()
                .toList();

        // Load all courses that have coordinates
        record Entry(Path file, String name, double lat, double lng) {}
        List<Entry> geoEntries = new ArrayList<>();
        int noCoords = 0;

        for (Path file : courseFiles) {
            try {
                String yaml = Files.readString(file);
                @SuppressWarnings("unchecked")
                Map<String, Object> data = yamlMapper.readValue(yaml, Map.class);
                String name = (String) data.get("name");
                Object latObj = data.get("lat");
                Object lngObj = data.get("lng");
                Double lat = toDouble(latObj);
                Double lng = toDouble(lngObj);
                if (lat == null || lng == null) {
                    log.warn("Skipping '{}': missing lat/lng", name);
                    noCoords++;
                    continue;
                }
                geoEntries.add(new Entry(file, name, lat, lng));
            } catch (Exception e) {
                log.warn("Could not parse {}: {}", file.getFileName(), e.getMessage());
            }
        }

        int computed = 0;

        for (Entry entry : geoEntries) {
            // Find 3 nearest (excluding self)
            List<String> nearest = geoEntries.stream()
                    .filter(other -> !other.file().equals(entry.file()))
                    .sorted(Comparator.comparingDouble(
                            other -> distanceKm(entry.lat(), entry.lng(), other.lat(), other.lng())))
                    .limit(3)
                    .map(Entry::name)
                    .toList();

            String nearby1 = !nearest.isEmpty() ? nearest.get(0) : null;
            String nearby2 = nearest.size() >= 2 ? nearest.get(1) : null;
            String nearby3 = nearest.size() >= 3 ? nearest.get(2) : null;

            try {
                String yaml = Files.readString(entry.file());
                String updated = updateNearbyInYaml(yaml, nearby1, nearby2, nearby3);
                Files.writeString(entry.file(), updated);
                log.info("Updated nearby courses for '{}'", entry.name());
                computed++;
            } catch (Exception e) {
                log.warn("Failed to write nearby courses for '{}': {}", entry.name(), e.getMessage());
            }
        }

        return String.format(
                "Computed nearby courses for %d course(s). %d course(s) skipped (missing lat/lng).",
                computed, noCoords);
    }

    /**
     * Haversine formula — returns distance in kilometres.
     */
    public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // -------------------------------------------------------------------------
    // YAML helpers
    // -------------------------------------------------------------------------

    /**
     * Replaces (or inserts) the nearby1/nearby2/nearby3 lines in the YAML content.
     * Coordinates are inserted after the lng: line when present; otherwise appended.
     */
    static String updateNearbyInYaml(String yamlContent, String nearby1, String nearby2, String nearby3) {
        String[] lines = yamlContent.split("\n");
        StringBuilder result = new StringBuilder();
        boolean addedNearby = false;

        for (String line : lines) {
            String trimmed = line.trim();
            // Remove existing nearby lines — will re-insert in the right place
            if (trimmed.startsWith("nearby1:") || trimmed.startsWith("nearby2:") || trimmed.startsWith("nearby3:")) {
                continue;
            }
            result.append(line).append("\n");
            // Insert after the lng: line
            if (!addedNearby && trimmed.startsWith("lng:")) {
                int indentLen = line.length() - line.stripLeading().length();
                String indent = line.substring(0, indentLen);
                appendNearby(result, indent, nearby1, nearby2, nearby3);
                addedNearby = true;
            }
        }

        // If there was no lng: line, append at the end
        if (!addedNearby) {
            appendNearby(result, "", nearby1, nearby2, nearby3);
        }

        return result.toString();
    }

    private static void appendNearby(StringBuilder sb, String indent,
                                     String nearby1, String nearby2, String nearby3) {
        if (nearby1 != null) sb.append(indent).append("nearby1: \"").append(nearby1).append("\"\n");
        if (nearby2 != null) sb.append(indent).append("nearby2: \"").append(nearby2).append("\"\n");
        if (nearby3 != null) sb.append(indent).append("nearby3: \"").append(nearby3).append("\"\n");
    }

    private static Double toDouble(Object obj) {
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        if (obj instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
