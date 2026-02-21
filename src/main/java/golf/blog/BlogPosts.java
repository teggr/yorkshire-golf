package golf.blog;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class BlogPosts {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final List<BlogPost> posts = new ArrayList<>();

    @PostConstruct
    @SneakyThrows
    public void onLoad() {
        BlogPost[] loaded = objectMapper.readValue(
                new ClassPathResource("blog-posts.json").getInputStream(),
                BlogPost[].class
        );
        posts.addAll(Arrays.asList(loaded));
        posts.sort((a, b) -> b.date().compareTo(a.date()));
    }

    public List<BlogPost> getAllPosts() {
        return new ArrayList<>(posts);
    }

    public BlogPost getPostById(String id) {
        return posts.stream()
                .filter(p -> p.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found: " + id));
    }

}
