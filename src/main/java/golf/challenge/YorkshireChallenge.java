package golf.challenge;

import golf.course.Course;
import golf.course.Courses;
import golf.course.Regions;
import golf.user.UserRound;
import golf.user.UserRoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class YorkshireChallenge {

    private final Courses courses;
    private final UserRoundRepository userRoundRepository;

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

}
