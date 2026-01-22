package be.steby.CoreProject.il.configs;

import be.steby.CoreProject.bll.domains.device.services.DeviceAuthenticationService;
import be.steby.CoreProject.bll.domains.auth.services.AuthService;
import be.steby.CoreProject.il.filters.JwtFilter;
import be.steby.CoreProject.il.Jwt.JwtUtil;
import be.steby.CoreProject.il.filters.MustChangePasswordFilter;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

// ✅ NEW: Import from SecurityRoutesAggregator instead of SecurityConstants
import static be.steby.CoreProject.il.routes.SecurityRoutesAggregator.PUBLIC_ROUTES;
import static be.steby.CoreProject.il.routes.SecurityRoutesAggregator.AUTHENTICATED_ROUTES;
import static be.steby.CoreProject.il.routes.SecurityRoutesAggregator.ADMIN_ROUTES;
import static be.steby.CoreProject.il.routes.SecurityRoutesAggregator.CSRF_IGNORE;

// TODO: These need to be migrated or defined properly
// Temporary imports from old SecurityConstants if they still exist
// Remove these after creating proper route files for them
import static be.steby.CoreProject.il.utils.SecurityConstants.ACTUATOR_PUBLIC_ROUTES;
import static be.steby.CoreProject.il.utils.SecurityConstants.MONITORING_AUTHORIZED_ROUTES;

/**
 * Spring Security configuration for the application.
 *
 * <p>This configuration defines:
 * <ul>
 *   <li>CSRF protection (active for authenticated routes, ignored for public routes)</li>
 *   <li>CORS configuration (allows frontend to communicate with backend)</li>
 *   <li>Authorization rules (public, authenticated, admin)</li>
 *   <li>JWT authentication filter chain</li>
 *   <li>OAuth2 login integration</li>
 *   <li>Session management (stateless for JWT)</li>
 * </ul>
 *
 * <p><b>Security Routes:</b>
 * Routes are now managed by domain-specific configurations in {@code il/routes/*}.
 * The {@link be.steby.CoreProject.il.routes.SecurityRoutesAggregator} combines
 * all domain routes and provides them to this configuration.
 *
 * <p><b>Migration Status:</b>
 * <ul>
 *   <li>✅ Main routes: Using SecurityRoutesAggregator</li>
 *   <li>⏳ ACTUATOR_PUBLIC_ROUTES: Still using old SecurityConstants (TODO: migrate)</li>
 *   <li>⏳ MONITORING_AUTHORIZED_ROUTES: Still using old SecurityConstants (TODO: migrate)</li>
 * </ul>
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

    // ⭐ Inject the custom OAuth2 success handler
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
    public JwtFilter jwtFilter(AuthService authService, JwtUtil jwtUtil, DeviceAuthenticationService deviceAuthenticationService) {
        return new JwtFilter(authService, jwtUtil, deviceAuthenticationService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtFilter jwtFilter, MustChangePasswordFilter mustChangePasswordFilter) throws Exception {
        http
                // ========== CSRF Configuration ==========
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        // ✅ UPDATED: Using SecurityRoutesAggregator.CSRF_IGNORE
                        .ignoringRequestMatchers(CSRF_IGNORE)  // Only safe public routes
                )

                // ========== CORS Configuration ==========
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ========== Authorization Configuration ==========
                .authorizeHttpRequests(auth -> auth
                        // 1. Public routes - no authentication required
                        // ✅ UPDATED: Using SecurityRoutesAggregator.PUBLIC_ROUTES
                        .requestMatchers(PUBLIC_ROUTES).permitAll()

                        // TODO: Migrate these to proper route files
                        .requestMatchers(ACTUATOR_PUBLIC_ROUTES).permitAll()
                        .requestMatchers(MONITORING_AUTHORIZED_ROUTES)
                        .hasAnyAuthority("MONITORING", "SUPER_ADMIN")

                        // 2. OPTIONS requests - allow for CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 3. OAuth2 routes - Spring Security OAuth2 endpoints
                        .requestMatchers(
                                "/oauth2/**",                    // Spring Security OAuth2 endpoints
                                "/login/oauth2/code/**"          // OAuth callback endpoint (GitHub, etc.)
                        ).permitAll()

                        // 4. Authenticated routes - require authentication but no specific role
                        // ✅ UPDATED: Using SecurityRoutesAggregator.AUTHENTICATED_ROUTES
                        .requestMatchers(AUTHENTICATED_ROUTES).authenticated()

                        // 5. Admin routes - require ADMIN or SUPER_ADMIN role
                        // ✅ UPDATED: Using SecurityRoutesAggregator.ADMIN_ROUTES
                        .requestMatchers(ADMIN_ROUTES).hasAnyAuthority("SUPER_ADMIN", "ADMIN")

                        // 6. Default - deny all other requests
                        .anyRequest().authenticated()
                )

                // ========== Session Management ==========
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // JWT-based, no sessions
                )

                // ========== Exception Handling ==========
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("Unauthorized access attempt to: {}", request.getRequestURI());
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                        })
                )

                // ========== OAuth2 Login Configuration ==========
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oauth2SuccessHandler)  // Custom success handler
                )

                // ========== JWT Filters ==========
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(mustChangePasswordFilter, JwtFilter.class);

        log.info("Security configuration loaded successfully");
        log.info("✅ Using SecurityRoutesAggregator for route configuration");
        log.info("⏳ TODO: Migrate ACTUATOR_PUBLIC_ROUTES and MONITORING_AUTHORIZED_ROUTES");

        return http.build();
    }

    /**
     * CORS configuration to allow frontend communication.
     *
     * <p>Allows requests from the frontend URL with credentials (cookies).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(FRONT_URL, BACK_URL));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);  // Allow cookies

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}