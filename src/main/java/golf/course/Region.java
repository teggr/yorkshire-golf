package golf.course;

import java.util.Arrays;
import java.util.stream.Collectors;

public record Region(String name) {

    public String displayName() {
        return Arrays.stream(name().split("_"))
                .map(s -> s.charAt(0) + s.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

}
