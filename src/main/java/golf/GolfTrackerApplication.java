package golf;

import j2html.rendering.IndentedHtml;
import j2html.tags.DomContent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.AbstractTemplateView;

import java.util.Map;

import static j2html.TagCreator.*;

@SpringBootApplication
public class GolfTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GolfTrackerApplication.class, args);
    }

}

@Controller
@RequestMapping("/")
class HomeController {

    @RequestMapping
    public String home() {
        return "homePage";
    }

}

@Component
class HomePage extends AbstractTemplateView {

    @Override
    protected boolean isUrlRequired() {
        return false;
    }

    @Override
    protected void renderMergedTemplateModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

        DomContent html = html(
                body(
                        h1("hello there")
                )
        );

        // write the document to the output stream
        join(document(),html).render(IndentedHtml.into(response.getWriter()));

    }

}