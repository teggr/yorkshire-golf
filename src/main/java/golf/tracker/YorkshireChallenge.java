package golf.tracker;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class YorkshireChallenge {

    private final Courses courses;
    private final CourseRecords courseRecords;

    public RegionChallengeTracker getTracker() {
        return new RegionChallengeTracker(
                courses.getCourseRegionCountGroupByRegion(),
                courseRecords.getCourseRecordsWhereRegionIn(
                        Regions.NorthYorkshire,
                        Regions.EastYorkshire,
                        Regions.SouthYorkshire,
                        Regions.WestYorkshire
                )
        );
    }

}
