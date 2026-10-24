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
            if (providers.secureCookies()) factory.getSettings().getSession().getCookie().setSecure(true);
        };
    }
}
