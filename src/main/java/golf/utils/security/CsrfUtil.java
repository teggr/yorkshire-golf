package golf.utils.security;

import jakarta.servlet.http.HttpServletRequest;
import j2html.tags.DomContent;
import org.springframework.security.web.csrf.CsrfToken;

import static j2html.TagCreator.input;
import static j2html.TagCreator.text;

public class CsrfUtil {

    public static DomContent csrfInput(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token != null) {
            return input().withType("hidden")
                    .withName(token.getParameterName())
                    .withValue(token.getToken());
        }
        return text("");
    }

}
