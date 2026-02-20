package be.steby.CoreProject.il.configs;

import be.steby.CoreProject.bll.domains.auth.services.jwt.AuthJwtService;
import be.steby.CoreProject.bll.domains.device.services.DeviceAuthenticationService;
import be.steby.CoreProject.bll.domains.auth.services.AuthService;
import be.steby.CoreProject.il.filters.JwtFilter;
import be.steby.CoreProject.il.filters.MustChangePasswordFilter;
import be.steby.CoreProject.il.filters.RateLimitFilter;
import be.steby.CoreProject.il.security.OAuth2AuthenticationSuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

// ✅ TOUS les imports depuis SecurityRoutesAggregator
import static be.steby.CoreProject.il.routes.SecurityRoutesAggregator.PUBLIC_ROUTES;
import static be.steby.CoreProject.il.routes.SecurityRoutesAggregator.AUTHENTICATED_ROUTES;
import static be.steby.CoreProject.il.routes.SecurityRoutesAggregator.ADMIN_ROUTES;
import static be.steby.CoreProject.il.routes.SecurityRoutesAggregator.MONITORING_AUTHORIZED_ROUTES;
import static be.steby.CoreProject.il.routes.SecurityRoutesAggregator.CSRF_IGNORE;

/**
 * Spring Security configuration for the application.
 *
 * <p>This configuration defines:
 * <ul>
 *   <li>CSRF protection (active for authenticated routes, ignored for public routes)</li>
 *   <li>CORS configuration (allows frontend to communicate with backend)</li>
 *   <li>Authorization rules (public, authenticated, monitoring, admin)</li>
 *   <li>JWT authentication filter chain</li>
 *   <li>OAuth2 login integration</li>
 *   <li>Session management (stateless for JWT)</li>
 * </ul>
 *
 * <p><b>Security Routes:</b>
 * All routes are managed by {@link be.steby.CoreProject.il.routes.SecurityRoutesAggregator}
 * which aggregates domain-specific route configurations.
 *
 * @author Steby Core Team
 * @since 2025-01
 * @see be.steby.CoreProject.il.routes.SecurityRoutesAggregator
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    @Value("${url.front_server}")
    private String FRONT_URL;

    @Value("${url.back_server}")
    private String BACK_URL;

    private final OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;

    public SecurityConfig(OAuth2AuthenticationSuccessHandler oauth2SuccessHandler) {
        this.oauth2SuccessHandler = oauth2SuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfig) throws Exception {
        return authenticationConfig.getAuthenticationManager();
    }

    @Bean
    public JwtFilter jwtFilter(AuthService authService, AuthJwtService authJwtService, DeviceAuthenticationService deviceAuthenticationService) {
        return new JwtFilter(authService, authJwtService, deviceAuthenticationService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtFilter jwtFilter,
                                                   MustChangePasswordFilter mustChangePasswordFilter,
                                                   RateLimitFilter rateLimitFilter) throws Exception {
        http
                // ========== CSRF Configuration ==========
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers(CSRF_IGNORE)
                )

                // ========== CORS Configuration ==========
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))


                // ========== Security Headers Configuration ==========
                .headers(headers -> headers
                        // Prevents browser from guessing Content-Type (prevents MIME sniffing attacks)
                        .contentTypeOptions(contentTypeOptions -> {})

                        // Prevents display in iframe (clickjacking protection)
                        .frameOptions(frameOptions -> frameOptions.deny())

                        // Enables browser XSS protection (legacy browsers)
                        .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))

                        // Forces HTTPS for 1 year (HSTS)
                        // Uncomment in production with valid SSL certificate
                        // .httpStrictTransportSecurity(hsts -> hsts
                        //     .includeSubDomains(true)
                        //     .maxAgeInSeconds(31536000)
                        // )

                        // Content Security Policy - adjust according to your needs
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'self'; " +
                                                "script-src 'self'; " +
                                                "style-src 'self' 'unsafe-inline'; " +
                                                "img-src 'self' data: https:; " +
                                                "font-src 'self' https://fonts.gstatic.com; " +
                                                "connect-src 'self' " + BACK_URL + "; " +
                                                "frame-ancestors 'none'; " +
                                                "form-action 'self';"
                                )
                        )

                        // Controls what the browser sends as Referer
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                        )

                        // Blocks sensitive features (camera, microphone, etc.)
                        .permissionsPolicyHeader(permissions -> permissions
                                .policy("geolocation=(self), camera=(self), microphone=(self)")
                        )
                )
                // ========== Authorization Configuration ==========
                .authorizeHttpRequests(auth -> auth
                        // 1. Public routes - no authentication required
                        .requestMatchers(PUBLIC_ROUTES).permitAll()

                        // 2. OPTIONS requests - allow for CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 3. OAuth2 routes - Spring Security OAuth2 endpoints
                        .requestMatchers(
                                "/oauth2/**",
                                "/login/oauth2/code/**"
                        ).permitAll()

                        // 4. Monitoring routes - requires MONITORING or SUPER_ADMIN authority
                        .requestMatchers(MONITORING_AUTHORIZED_ROUTES)
                        .hasAnyAuthority("MONITORING", "SUPER_ADMIN")

                        // 5. Authenticated routes - require authentication but no specific role
                        .requestMatchers(AUTHENTICATED_ROUTES).authenticated()

                        // 6. Admin routes - require ADMIN or SUPER_ADMIN role
                        .requestMatchers(ADMIN_ROUTES).hasAnyAuthority("SUPER_ADMIN", "ADMIN")

                        // 7. Default - require authentication for any other request
                        .anyRequest().authenticated()
                )

                // ========== Session Management ==========
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // ========== Exception Handling ==========
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("Unauthorized access attempt to: {}", request.getRequestURI());
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write(
                                    "{\"error\":\"unauthorized\",\"message\":\"Authentication required\"}"
                            );
                        })
                )

                // ========== Logout ==========
                .logout(logout -> logout.disable())

                // ========== OAuth2 Login Configuration ==========
                .oauth2Login(oauth2 -> oauth2
                        .permitAll()
                        .successHandler(oauth2SuccessHandler)
                        .failureHandler((request, response, exception) -> {
                            log.error("OAuth2 authentication failed", exception);
                            response.sendRedirect(FRONT_URL + "/auth/login?error=oauth2");
                        })
                )

                // ========== Filters ==========
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(mustChangePasswordFilter, JwtFilter.class)
                .addFilterAfter(rateLimitFilter, MustChangePasswordFilter.class);

        log.info("Security configuration loaded successfully");
        log.info("✅ Using SecurityRoutesAggregator for all route configurations");

        return http.build();
    }

    /**
     * CORS configuration to allow frontend communication.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(FRONT_URL, BACK_URL));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-XSRF-TOKEN"));
        configuration.setExposedHeaders(List.of("X-Correlation-ID", "Retry-After"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}