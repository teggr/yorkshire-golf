package golf.challenge;

import golf.course.Course;
import golf.course.Courses;
import golf.course.Regions;
import golf.user.UserRound;
import golf.user.UserRoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class YorkshireChallenge {

    private final Courses courses;
    private final UserRoundRepository userRoundRepository;
    private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy", Locale.UK);

    public RegionChallengeTracker getTrackerForUser(Long userId) {
        List<UserRound> userRounds = userRoundRepository.findByUserId(userId);
        List<UserRound> enrichedRounds = userRounds.stream()
                .map(r -> {
                    Course course = courses.getCourseByName(r.courseName());
                    if (course == null || !List.of(
                            Regions.NorthYorkshire, Regions.EastYorkshire,
                            Regions.SouthYorkshire, Regions.WestYorkshire
                    ).contains(course.region())) {
                        return null;
                    }
                    return new UserRound(r.id(), r.userId(), r.title(), r.date(),
                            r.courseName(), course, r.imageUrls(), r.content());
                })
                .filter(java.util.Objects::nonNull)
                .toList();
        return new RegionChallengeTracker(
                courses.getCourseRegionCountGroupByRegion(),
                enrichedRounds
        );
    }

    public MonthlyCourseProgress getMonthlyCourseProgressForUser(Long userId) {
        RegionChallengeTracker tracker = getTrackerForUser(userId);
        List<UserRound> sortedRounds = tracker.rounds().stream()
                .sorted(java.util.Comparator.comparing(r -> LocalDate.parse(r.date())))
                .toList();

        if (sortedRounds.isEmpty()) {
            return new MonthlyCourseProgress(List.of(), List.of());
        }

        YearMonth start = YearMonth.from(LocalDate.parse(sortedRounds.get(0).date()));
        YearMonth end = YearMonth.from(LocalDate.parse(sortedRounds.get(sortedRounds.size() - 1).date()));

        Set<String> seenCourses = new HashSet<>();
        Map<YearMonth, Long> newlyPlayedByMonth = new TreeMap<>();
        for (UserRound round : sortedRounds) {
            if (!seenCourses.add(round.courseName())) {
                continue;
            }
            YearMonth month = YearMonth.from(LocalDate.parse(round.date()));
            newlyPlayedByMonth.merge(month, 1L, Long::sum);
        }

        List<String> labels = new java.util.ArrayList<>();
        List<Long> cumulative = new java.util.ArrayList<>();
        long runningTotal = 0L;
        YearMonth month = start;
        while (!month.isAfter(end)) {
            runningTotal += newlyPlayedByMonth.getOrDefault(month, 0L);
            labels.add(month.format(MONTH_LABEL_FORMATTER));
            cumulative.add(runningTotal);
            month = month.plusMonths(1);
        }

        return new MonthlyCourseProgress(labels, cumulative);
    }

}
