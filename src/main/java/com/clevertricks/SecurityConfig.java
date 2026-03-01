package com.clevertricks;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${KEYCLOAK_END_SESSION_URI:http://localhost:8180/realms/filevault/protocol/openid-connect/logout}")
    private String endSessionUri;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2.defaultSuccessUrl("/", true))
                .logout(logout -> logout
                        .logoutSuccessHandler(keycloakLogoutHandler())
                        .invalidateHttpSession(true)
                        .clearAuthentication(true));
        return http.build();
    }

    private LogoutSuccessHandler keycloakLogoutHandler() {
        return (request, response, authentication) -> {
            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            StringBuilder logoutUrl = new StringBuilder(endSessionUri);
            logoutUrl.append("?post_logout_redirect_uri=").append(baseUrl);
            if (authentication instanceof OAuth2AuthenticationToken token) {
                OidcUser user = (OidcUser) token.getPrincipal();
                logoutUrl.append("&id_token_hint=").append(user.getIdToken().getTokenValue());
            }
            response.sendRedirect(logoutUrl.toString());
        };
    }
}
