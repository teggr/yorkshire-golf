package golf.challenge;

import golf.course.Courses;
import golf.course.Regions;
import golf.round.Rounds;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class YorkshireChallenge {

    private final Courses courses;
    private final Rounds rounds;

    public RegionChallengeTracker getTracker() {
        return new RegionChallengeTracker(
                courses.getCourseRegionCountGroupByRegion(),
                rounds.getRoundsWhereRegionIn(
                        Regions.NorthYorkshire,
                        Regions.EastYorkshire,
                        Regions.SouthYorkshire,
                        Regions.WestYorkshire
                )
        );
    }

}
