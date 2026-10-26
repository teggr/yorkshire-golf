package golf.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class PrivacyController {
    private final PrivacyPage privacyPage;
    private final DataDeletionPage dataDeletionPage;

    public PrivacyController(PrivacyPage privacyPage, DataDeletionPage dataDeletionPage) {
        this.privacyPage = privacyPage;
        this.dataDeletionPage = dataDeletionPage;
    }

    @GetMapping("/privacy")
    public ModelAndView privacy() {
        return new ModelAndView(privacyPage);
    }

    @GetMapping("/data-deletion")
    public ModelAndView dataDeletion() {
        return new ModelAndView(dataDeletionPage);
    }
}
