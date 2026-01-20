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

import static be.steby.CoreProject.il.utils.SecurityConstants.*;

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
                        .ignoringRequestMatchers(CSRF_IGNORE_PATHS)  // ✅ Only public routes
                )

                // ========== CORS Configuration ==========
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ========== Authorization Configuration ==========
                .authorizeHttpRequests(auth -> auth
                        // 1. Public routes - no authentication required
                        .requestMatchers(PUBLIC_ROUTES).permitAll()
                        .requestMatchers(ACTUATOR_PUBLIC_ROUTES).permitAll()

                        // 2. OPTIONS requests - allow for CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ⭐ 3. OAuth2 routes - CORRECTION ICI
                        .requestMatchers(
                                "/oauth2/**",                    // Spring Security OAuth2 endpoints
                                "/login/oauth2/code/**"          // GitHub callback endpoint
                        ).permitAll()

                        // 3. Authenticated routes - require authentication but no specific role
                        .requestMatchers(AUTHENTICATED_ROUTES).authenticated()  // ✅ Explicit!

                        // 4. Admin routes - require ADMIN or SUPER_ADMIN role
                        .requestMatchers(ADMIN_ROUTES).hasAnyAuthority("SUPER_ADMIN", "ADMIN")

                        // 5. All other requests - require authentication (fallback)
                        .anyRequest().authenticated()
                )

                // ========== Exception Handling  ==========
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            // Return 401 JSON instead of redirecting to /login
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write(
                                    "{\"error\":\"unauthorized\",\"message\":\"Authentication required\"}"
                            );
                        })
                )

                // ========== Session Management ==========
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // ========== Logout ==========
                .logout(logout -> logout.disable())  // Custom logout in controller

                // ⭐ ========== OAuth2 Login Configuration - CORRECTION ICI ==========
                .oauth2Login(oauth2 -> oauth2
                        // Allow access without authentication
                        .permitAll()

                        // ✅ Use custom success handler
                        .successHandler(oauth2SuccessHandler)

                        // Redirect to front in case of failure
                        .failureHandler((request, response, exception) -> {
                            log.error("OAuth2 authentication failed", exception);
                            response.sendRedirect(FRONT_URL + "/auth/login?error=oauth2");
                        })
                )

                // ========== JWT Filter ==========
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(mustChangePasswordFilter, JwtFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(FRONT_URL, BACK_URL));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setExposedHeaders(List.of("X-Correlation-ID"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}