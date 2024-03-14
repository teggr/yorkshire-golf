package golf.utils.j2html;

import org.springframework.web.servlet.view.AbstractTemplateView;

public abstract class J2HtmlTemplateView extends AbstractTemplateView {

    @Override
    protected boolean isUrlRequired() {
        return false;
    }

}
