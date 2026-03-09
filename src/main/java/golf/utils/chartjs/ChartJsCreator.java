package golf.utils.chartjs;

import j2html.tags.InlineStaticResource;
import j2html.tags.UnescapedText;
import j2html.tags.specialized.ScriptTag;
import lombok.SneakyThrows;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.script;

public class ChartJsCreator {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static ScriptTag chartJsLibScript() {
        return script()
                .withSrc("https://cdn.jsdelivr.net/npm/chart.js@3.9.1");
    }

    public static ScriptTag chartJsPluginDataLabelsLibScript() {
        return script()
                .withSrc("https://cdn.jsdelivr.net/npm/chartjs-plugin-datalabels@2.2.0");
    }

    public static ScriptTag chartJsPluginDoughnutLabelLibScript() {
        return script()
                .withSrc("https://cdn.jsdelivr.net/npm/chartjs-plugin-doughnutlabel-v3@1.2.0");
    }

    // https://www.chartjs.org/docs/latest/charts/doughnut.html
    @SneakyThrows
    public static ScriptTag chartJsConfigScript(String id, Map<String, Object> chartData) {
        return chartJsConfigScript(id, chartData, "/progress-chart.js");
    }

    @SneakyThrows
    public static ScriptTag chartJsConfigScript(String id, Map<String, Object> chartData, String resourcePath) {

        String fileAsString = InlineStaticResource.getFileAsString(resourcePath)
                .replaceAll("myChart", id);

        for (Map.Entry<String, Object> e : chartData.entrySet()) {

            fileAsString = fileAsString.replaceAll(e.getKey(), objectMapper.writeValueAsString(e.getValue()));

        }

        UnescapedText unescapedText = rawHtml(fileAsString);

        return script(unescapedText);

    }

}
