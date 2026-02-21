package golf.challenge;

import golf.course.Course;

import java.time.LocalDate;

public record CourseRecord(Course course, LocalDate datePlayed) {
}
