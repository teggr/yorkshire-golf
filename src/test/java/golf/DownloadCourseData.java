package golf;

import golf.tracker.Region;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

@Slf4j
public class DownloadCourseData {

    public static void main(String[] args) throws IOException {

        var regionListings = List.of(
                new RegionForDownload(new Region("NORTH_YORKSHIRE"), "https://www.golfshake.com/course/Europe/United_Kingdom/England/North_East/North_Yorkshire/"),
                new RegionForDownload(new Region("EAST_YORKSHIRE"), "https://www.golfshake.com/course/Europe/United_Kingdom/England/North_East/East_Yorkshire/"),
                new RegionForDownload(new Region("SOUTH_YORKSHIRE"), "https://www.golfshake.com/course/Europe/United_Kingdom/England/North_East/South_Yorkshire/"),
                new RegionForDownload(new Region("WEST_YORKSHIRE"), "https://www.golfshake.com/course/Europe/United_Kingdom/England/North_East/West_Yorkshire/")
        );

        StringBuffer stringBuffer = new StringBuffer();

        stringBuffer.append("COURSE_NAME,REGION\n");

        regionListings.forEach(regionListing -> {

            try {

                Document doc = Jsoup.connect(regionListing.url()).get();
                log.info(doc.title());
                Element listingsTable = doc.selectFirst("table.listing");

                Elements listingRows = listingsTable.select("tbody>tr:has(td[rowspan=2])");

                log.info("{} listings", listingRows.size());

                listingRows.forEach(lr -> {
                    String courseName = lr.child(1).select("a").text();

                    log.info("{}", courseName);

                    stringBuffer.append(courseName);
                    stringBuffer.append(",");
                    stringBuffer.append(regionListing.region().name());
                    stringBuffer.append("\n");

                });

            } catch (Exception e) {

                e.printStackTrace();

            }

        });

        Files.writeString(Path.of("./target/courses.csv"), stringBuffer.toString(), StandardOpenOption.CREATE_NEW, StandardOpenOption.TRUNCATE_EXISTING);

    }

    record RegionForDownload(Region region, String url) {
    }

    ;

}
