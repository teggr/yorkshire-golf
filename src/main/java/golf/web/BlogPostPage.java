package golf.web;

import golf.blog.BlogPost;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.View;

import java.util.Map;

import static j2html.TagCreator.*;

@Component
@Slf4j
public class BlogPostPage implements View {

    @Override
    public @Nullable String getContentType() {
        return MediaType.TEXT_HTML_VALUE;
    }

    @Override
    public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

        BlogPost post = (BlogPost) model.get("post");

        new YorkshireGolfPageTemplate()
                .withTitle(post.title() + " - Yorkshire Golf Life")
                .withBody(
                        div().withClass("container py-4").with(
                                a("← Back to blog").withHref("/blog").withClass("text-decoration-none text-muted mb-4 d-inline-block"),
                                div().withClass("row justify-content-center").with(
                                        div().withClass("col-lg-8").with(
                                                article().with(
                                                        h1(post.title()).withClass("display-6 mb-1"),
                                                        p().withClass("text-muted mb-3").with(
                                                                span(post.date()),
                                                                post.courseName() != null && !post.courseName().isBlank()
                                                                        ? span().with(
                                                                                text(" · "),
                                                                                span(post.courseName()).withClass("badge bg-dark fw-normal")
                                                                          )
                                                                        : span("")
                                                        ),
                                                        post.imageUrl() != null && !post.imageUrl().isBlank()
                                                                ? img().withSrc(post.imageUrl())
                                                                        .withAlt(post.title())
                                                                        .withClass("img-fluid rounded mb-4 w-100")
                                                                : div(),
                                                        div().withClass("fs-5 lh-lg").with(
                                                                p(post.content())
                                                        )
                                                )
                                        )
                                )
                        )
                )
                .render(response.getWriter());

        log.info("BlogPostPage rendered for post: {}", post.id());
    }

}
