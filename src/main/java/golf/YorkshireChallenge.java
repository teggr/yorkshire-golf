package golf;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class YorkshireChallenge {

    private static final List<CourseRecord> courseRecords = new ArrayList<>();
    private static final Map<Region, Integer> courseCountByRegion = new HashMap<>();

    static {

        courseRecords.add(new CourseRecord(new Course("Moortown Golf Club", new Region("WEST_YORKSHIRE")), LocalDate.of(2023, 4, 9)));
        courseRecords.add(new CourseRecord(new Course("Rudding Park Golf Club", new Region("NORTH_YORKSHIRE")), LocalDate.of(2023, 5, 13)));
        courseRecords.add(new CourseRecord(new Course("Scarcroft Golf Club", new Region("WEST_YORKSHIRE")), LocalDate.of(2023, 7, 6)));
        courseRecords.add(new CourseRecord(new Course("De Vere Oulton Hall Golf Club", new Region("WEST_YORKSHIRE")), LocalDate.of(2023, 7, 29)));
        courseRecords.add(new CourseRecord(new Course("Spofforth Golf Course", new Region("NORTH_YORKSHIRE")), LocalDate.of(2023, 9, 5)));
        courseRecords.add(new CourseRecord(new Course("Ganton Golf Club", new Region("NORTH_YORKSHIRE")), LocalDate.of(2023, 9, 6)));
        courseRecords.add(new CourseRecord(new Course("Leeds Golf Club", new Region("WEST_YORKSHIRE")), LocalDate.of(2023, 9, 8)));
        courseRecords.add(new CourseRecord(new Course("Calverley Golf Club", new Region("WEST_YORKSHIRE")), LocalDate.of(2023, 9, 22)));
        courseRecords.add(new CourseRecord(new Course("Headingly Golf Club", new Region("WEST_YORKSHIRE")), LocalDate.of(2023, 9, 25)));
        courseRecords.add(new CourseRecord(new Course("Leeds Golf Centre", new Region("WEST_YORKSHIRE")), LocalDate.of(2024, 1, 27)));
        courseRecords.add(new CourseRecord(new Course("Tadcaster Golf Club", new Region("NORTH_YORKSHIRE")), LocalDate.of(2024, 2, 25)));

        courseCountByRegion.put(new Region("WEST_YORKSHIRE"), 88);
        courseCountByRegion.put(new Region("NORTH_YORKSHIRE"), 58);
        courseCountByRegion.put(new Region("SOUTH_YORKSHIRE"), 43);
        courseCountByRegion.put(new Region("EAST_YORKSHIRE"), 19);

    }

    public CourseTracker getTracker() {
        return new CourseTracker( courseCountByRegion, courseRecords);
    }

}
