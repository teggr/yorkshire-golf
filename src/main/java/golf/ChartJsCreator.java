package golf;

import j2html.tags.InlineStaticResource;
import j2html.tags.UnescapedText;
import j2html.tags.specialized.ScriptTag;

import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.script;

public class ChartJsCreator {

    public static ScriptTag chartJs() {
        return script()
                .withSrc("https://cdn.jsdelivr.net/npm/chart.js");
    }

    public static ScriptTag chartJsChart(String id, Options options) {

        String fileAsString = InlineStaticResource.getFileAsString("/progress-chart.js")
                .replaceAll("'myChart'", "'" + id + "'")
                .replaceAll("text: '8% Complete'", "text: '" + options.getText() + "'")
                .replaceAll("color: '#FF6384'", "color: '" + options.getColor() + "'")
                .replaceAll("fontStyle: 'Arial'", "fontStyle: '" + options.getFontStyle() + "'")
                .replaceAll("sidePadding: 20", "sidePadding: " + options.getSidePadding() )
                .replaceAll("minFontSize: 20", "minFontSize: " + options.getMinFontSize() )
                .replaceAll("lineHeight: 25", "lineHeight: " + options.getLineHeight() );

        UnescapedText unescapedText = rawHtml(fileAsString);

        return script(unescapedText);

    }

}
