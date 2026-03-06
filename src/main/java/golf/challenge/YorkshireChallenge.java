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
        List<Course> playedCourses = userRounds.stream()
                .map(r -> courses.getCourseByName(r.courseName()))
                .filter(c -> c != null && List.of(
                        Regions.NorthYorkshire, Regions.EastYorkshire,
                        Regions.SouthYorkshire, Regions.WestYorkshire
                ).contains(c.region()))
                .toList();
        return new RegionChallengeTracker(
                courses.getCourseRegionCountGroupByRegion(),
                playedCourses
        );
    }

}
