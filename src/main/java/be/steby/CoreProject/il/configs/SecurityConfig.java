package be.steby.CoreProject.il.configs;

import be.steby.CoreProject.bll.domains.device.services.DeviceAuthenticationService;
import be.steby.CoreProject.bll.domains.auth.services.AuthService;
import be.steby.CoreProject.il.filters.JwtFilter;
import be.steby.CoreProject.il.Jwt.JwtUtil;
import be.steby.CoreProject.il.filters.MustChangePasswordFilter;
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
public class SecurityConfig {

    @Value("${url.front_server}")
    private String FRONT_URL;

    @Value("${url.back_server}")
    private String BACK_URL;

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

                        // 2. OPTIONS requests - allow for CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 3. Authenticated routes - require authentication but no specific role
                        .requestMatchers(AUTHENTICATED_ROUTES).authenticated()  // ✅ Explicit!

                        // 4. Admin routes - require ADMIN or SUPER_ADMIN role
                        .requestMatchers(ADMIN_ROUTES).hasAnyAuthority("SUPER_ADMIN", "ADMIN")

                        // 5. All other requests - require authentication (fallback)
                        .anyRequest().authenticated()
                )

                // ========== Session Management ==========
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // ========== Logout ==========
                .logout(logout -> logout.disable())  // Custom logout in controller

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
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}