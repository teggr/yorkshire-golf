package golf;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Options {

    @Builder.Default
    String text = "";
    @Builder.Default
    String color = "#000000";
    @Builder.Default
    String fontStyle = "Arial";
    @Builder.Default
    Integer sidePadding = 20;
    @Builder.Default
    Integer minFontSize = 20;
    @Builder.Default
    Integer lineHeight = 25;

    JsonNode data;

}
