package golf.course;

import org.jspecify.annotations.Nullable;

public record Course(String name, Region region, @Nullable String website, @Nullable String mainImageUrl, boolean closed) {
}
