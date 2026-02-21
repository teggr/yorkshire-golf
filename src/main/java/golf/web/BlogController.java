package golf.web;

import golf.blog.BlogPosts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/blog")
@RequiredArgsConstructor
public class BlogController {

    private final BlogPosts blogPosts;

    @GetMapping
    public String blog(Model model) {
        model.addAttribute("posts", blogPosts.getAllPosts());
        return "blogPage";
    }

    @GetMapping("/{id}")
    public String post(@PathVariable String id, Model model) {
        model.addAttribute("post", blogPosts.getPostById(id));
        return "blogPostPage";
    }

}
