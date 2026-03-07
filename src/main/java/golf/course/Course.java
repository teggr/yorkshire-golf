package golf.course;

import org.jspecify.annotations.Nullable;

public record Course(String name,
					 Region region,
					 @Nullable String website,
					 @Nullable String mainImageUrl,
					 @Nullable String stayImageUrl,
					 boolean closed,
					 boolean playAndStay,
					 @Nullable String address,
					 @Nullable Double lat,
					 @Nullable Double lng,
					 @Nullable String nearby1,
					 @Nullable String nearby2,
					 @Nullable String nearby3,
					 @Nullable Integer top100,
					 @Nullable Integer next100) {
}
