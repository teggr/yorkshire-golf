package golf.round;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/rounds")
@RequiredArgsConstructor
public class RoundsController {

    private final Rounds rounds;

    @GetMapping
    public String rounds(Model model) {
        model.addAttribute("rounds", rounds.getAllRounds());
        return "roundsPage";
    }

    @GetMapping("/{id}")
    public String round(@PathVariable String id, Model model) {
        model.addAttribute("round", rounds.getRoundById(id));
        return "roundDetailPage";
    }

}
