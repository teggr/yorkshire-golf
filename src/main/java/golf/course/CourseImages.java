package golf.course;

import org.jspecify.annotations.Nullable;

public final class CourseImages {

    private static final String COURSES_IMAGE_PREFIX = "/images/courses/";
    private static final String COURSES_THUMBS_PREFIX = "/images/courses/thumbs/";

    private CourseImages() {
    }

    public static @Nullable String toThumbUrl(@Nullable String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank() || !imageUrl.startsWith(COURSES_IMAGE_PREFIX)) {
            return imageUrl;
        }
        if (imageUrl.startsWith(COURSES_THUMBS_PREFIX)) {
            return imageUrl;
        }

        int lastSlash = imageUrl.lastIndexOf('/');
        int lastDot = imageUrl.lastIndexOf('.');
        if (lastDot <= lastSlash) {
            return imageUrl;
        }

        String baseName = imageUrl.substring(lastSlash + 1, lastDot);
        return COURSES_THUMBS_PREFIX + baseName + ".jpg";
    }
}