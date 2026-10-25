package golf.security;

import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AuthenticationCookies {
    @Bean
    WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> authenticationCookieCustomizer(
            FederatedProviders providers) {
        return factory -> {
            factory.getSettings().getSession().getCookie().setHttpOnly(true);
            boolean apple = providers.enabled().contains("apple");
            if (providers.secureCookies() || apple) factory.getSettings().getSession().getCookie().setSecure(true);
            factory.getSettings().getSession().getCookie().setSameSite(apple
                    ? org.springframework.boot.web.server.Cookie.SameSite.NONE
                    : org.springframework.boot.web.server.Cookie.SameSite.LAX);
        };
    }
}
