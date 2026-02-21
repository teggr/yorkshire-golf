package golf.web;

import golf.blog.BlogPost;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.View;

import java.util.List;
import java.util.Map;

import static j2html.TagCreator.*;

@Component
@Slf4j
public class BlogPage implements View {

    @Override
    public @Nullable String getContentType() {
        return MediaType.TEXT_HTML_VALUE;
    }

    @Override
    public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

        @SuppressWarnings("unchecked")
        List<BlogPost> posts = (List<BlogPost>) model.get("posts");

        new YorkshireGolfPageTemplate()
                .withTitle("Yorkshire Golf Life - Blog")
                .withBody(
                        div().withClass("container py-4").with(
                                h1("Golf Life Blog").withClass("display-6 mb-2"),
                                p("Progress, rounds and reflections on the Yorkshire Golf Challenge.").withClass("lead mb-5"),
                                div().withClass("row row-cols-1 row-cols-md-2 g-4").with(
                                        posts.stream().map(post ->
                                                div().withClass("col").with(
                                                        div().withClass("card h-100").with(
                                                                div().withClass("card-body").with(
                                                                        h5(post.title()).withClass("card-title"),
                                                                        p().withClass("card-text text-muted small mb-2").with(
                                                                                span(post.date()),
                                                                                post.courseName() != null && !post.courseName().isBlank()
                                                                                        ? span(" · " + post.courseName())
                                                                                        : span("")
                                                                        ),
                                                                        p(post.content().length() > 160
                                                                                ? post.content().substring(0, 160) + "…"
                                                                                : post.content()
                                                                        ).withClass("card-text"),
                                                                        a("Read more →")
                                                                                .withClass("btn btn-sm btn-outline-dark mt-2")
                                                                                .withHref("/blog/" + post.id())
                                                                )
                                                        )
                                                )
                                        ).toArray(j2html.tags.DomContent[]::new)
                                )
                        )
                )
                .render(response.getWriter());

        log.info("BlogPage rendered");
    }

}
