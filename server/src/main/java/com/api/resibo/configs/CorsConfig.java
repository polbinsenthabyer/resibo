package com.api.resibo.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Which browser origins may call this API.
 *
 * <p>Separate from {@link SecurityConfig} on purpose. CORS answers a different
 * question — "may this page read the response?" — from authentication and
 * authorisation, it changes for different reasons (a new frontend origin, not
 * a new role), and keeping it here means a change to the allow-list cannot
 * accidentally disturb the filter chain.
 *
 * <p>{@code SecurityConfig} still has to opt in with {@code .cors(...)}; this
 * class only supplies the {@link CorsConfigurationSource} bean that Spring
 * Security then picks up from the context.
 *
 * <p><b>CORS is a browser rule, not a firewall.</b> It stops a page on another
 * origin from reading responses; it does nothing to curl, Postman, or any
 * server-to-server caller, which send no {@code Origin} header and are
 * unaffected. Authentication is what protects the data — see
 * {@code SecurityConfig}.
 */
@Configuration
public class CorsConfig {

    /**
     * Comma-separated origins, from {@code CORS_ALLOWED_ORIGINS}.
     *
     * <p>The default lives in {@code application.yaml} and is the real list,
     * deliberately not {@code "*"} — an environment that forgets the variable
     * should fail closed. {@code http://localhost:5173} is in the production
     * list on purpose: the client defaults to calling the deployed API, so
     * {@code npm run dev} is a cross-origin call from localhost.
     */
    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();

        // Browsers reject credentialed requests from a "*" origin, so only
        // allow credentials once real origins are named. Resibo does not need
        // them today — the token goes in the Authorization header, not a
        // cookie — but this means switching to named origins does not quietly
        // leave cookies broken later.
        boolean wildcard = origins.contains("*");

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(!wildcard);
        // Lets a browser read the header back on a cross-origin response.
        config.setExposedHeaders(List.of("Authorization"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
