package be.steby.CoreProject.utils;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Minimal Spring Security configuration for @WebMvcTest slices.
 *
 * <p>Replaces the production SecurityConfig which is too complex for test slices
 * (requires OAuth2, JWT filters, rate limiting, etc.).
 *
 * <p>Behaviour:
 * <ul>
 *   <li>CSRF disabled — simplifies test request building</li>
 *   <li>All paths permitted — path-level security not tested here</li>
 *   <li>@EnableMethodSecurity — keeps @PreAuthorize annotations active</li>
 * </ul>
 *
 * <p>Usage: {@code @Import(WebMvcTestSecurityConfig.class)} on the test class,
 * combined with excluding the production SecurityConfig via
 * {@code @WebMvcTest(excludeFilters = ...)}.
 */
@TestConfiguration
@EnableMethodSecurity
public class WebMvcTestSecurityConfig {

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
