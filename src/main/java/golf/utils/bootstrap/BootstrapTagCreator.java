package golf.utils.bootstrap;

import j2html.tags.specialized.LinkTag;
import j2html.tags.specialized.MetaTag;
import j2html.tags.specialized.ScriptTag;

import static j2html.TagCreator.*;

public class BootstrapTagCreator {

    public static MetaTag bootstrapCharsetMetaTag() {
        return meta()
                .attr("charset", "utf-8");
    }

    public static MetaTag bootstrapViewportMetaTag() {
        return meta()
                .withName("viewport")
                .attr("content", "width=device-width, initial-scale=1");
    }

    public static LinkTag bootstrapMinCssLinkTag() {
        return link()
                .withHref("https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css")
                .attr("rel", "stylesheet")
                .attr("integrity", "sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH")
                .attr("crossorigin", "anonymous");
    }

    public static ScriptTag popperMinJsScriptTag() {
        return script()
                .attr("src", "https://cdn.jsdelivr.net/npm/@popperjs/core@2.11.8/dist/umd/popper.min.js")
                .attr("integrity", "sha384-I7E8VVD/ismYTF4hNIPjVp/Zjvgyol6VFvRkX/vR+Vc4jQkC+hVqc2pM8ODewa9r")
                .attr("crossorigin", "anonymous");
    }

    public static ScriptTag bootstrapMinJsScriptTag() {
        return script()
                .attr("src", "https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.min.js")
                .attr("integrity", "sha384-0pUGZvbkm6XF6gxjEnlmuGrJXVbNuzT9qBBavbLwCsOGabYfZo0T0to5eqruptLy")
                .attr("crossorigin", "anonymous");
    }

}
