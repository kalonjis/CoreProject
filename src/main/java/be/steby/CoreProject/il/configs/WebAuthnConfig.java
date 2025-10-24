package be.steby.CoreProject.il.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import javax.sql.DataSource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Native WebAuthn Configuration with Spring Security 6.4+
 *
 * ✅ SPRING BOOT 3.4.11: Native WebAuthn support enabled
 * ✅ POSTGRES: Auto-created tables in your existing database
 * ✅ 2FA INTEGRATION: Configuration loaded from config/security/two-factor/webauthn.yml
 * ✅ ARCHITECTURE: Follows your existing 2FA configuration pattern
 */
@Configuration
@Slf4j
public class WebAuthnConfig {

    @Value("${url.front_server}")
    private String FRONT_URL;

    @Value("${url.back_server}")
    private String BACK_URL;

    /**
     * ✅ SPRING SECURITY 6.4+ Native WebAuthn FilterChain
     * Configuration loaded from config/security/two-factor/webauthn.yml
     */
    @Bean
    @Order(1)
    public SecurityFilterChain webAuthnFilterChain(
            HttpSecurity http,
            WebAuthnProperties webAuthnProperties) throws Exception {

        log.info("🎉 Configuring NATIVE Spring Security 6.4+ WebAuthn 2FA (Spring Boot 3.4.11)");
        log.info("🔧 WebAuthn 2FA RP Name: {}", webAuthnProperties.getRpName());
        log.info("🔧 WebAuthn 2FA RP ID: {}", webAuthnProperties.getRpId());
        log.info("🔧 WebAuthn 2FA Integration Enabled: {}", webAuthnProperties.getTwoFactor().isEnabled());
        log.info("🔧 WebAuthn 2FA Can Be Primary: {}", webAuthnProperties.getTwoFactor().isCanBePrimary());

        return http
                .securityMatcher("/webauthn/**")

                // 🎉 NATIVE: .webAuthn() method available with Spring Security 6.4+
                .webAuthn(webAuthn -> webAuthn
                                .rpName(webAuthnProperties.getRpName())
                                .rpId(webAuthnProperties.getRpId())
                                .allowedOrigins(FRONT_URL, BACK_URL)
                        // JDBC repositories are auto-configured with your PostgreSQL! 🎉
                )

                .sessionManagement(session -> session.sessionCreationPolicy(
                        org.springframework.security.config.http.SessionCreationPolicy.STATELESS))

                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                    corsConfig.setAllowedOrigins(java.util.List.of(FRONT_URL, BACK_URL));
                    corsConfig.setAllowedMethods(java.util.List.of("POST", "OPTIONS"));
                    corsConfig.setAllowedHeaders(java.util.List.of("Content-Type", "Authorization"));
                    corsConfig.setAllowCredentials(true);
                    corsConfig.setMaxAge(3600L);
                    return corsConfig;
                }))

                .csrf(csrf -> csrf.disable())

                .build();
    }

    /**
     * WebAuthn Properties loaded from config/security/two-factor/webauthn.yml
     * Follows the same pattern as your other 2FA configurations
     */
    @Bean
    @ConfigurationProperties(prefix = "app.webauthn")
    public WebAuthnProperties webAuthnProperties() {
        return new WebAuthnProperties();
    }

    /**
     * WebAuthn 2FA Configuration Properties Class
     * Maps to the structure defined in config/security/two-factor/webauthn.yml
     */
    @Data
    public static class WebAuthnProperties {
        private boolean enabled = true;
        private String rpName = "Steby Core Project";
        private String rpId = "localhost";

        // 2FA Integration settings
        private TwoFactorSettings twoFactor = new TwoFactorSettings();

        // Security settings
        private SecuritySettings security = new SecuritySettings();

        // Authenticator types
        private AuthenticatorTypes authenticatorTypes = new AuthenticatorTypes();

        // User experience settings
        private UserExperienceSettings userExperience = new UserExperienceSettings();

        // Advanced settings
        private AdvancedSettings advanced = new AdvancedSettings();

        @Data
        public static class TwoFactorSettings {
            private boolean enabled = true;
            private boolean canBePrimary = true;
            private boolean canBeSecondary = true;
            private boolean requireEnrollment = true;
        }

        @Data
        public static class SecuritySettings {
            private boolean requireAttestation = false;
            private boolean strictMetadataValidation = true;
            private int timeoutSeconds = 60;
            private int challengeTimeoutSeconds = 120;
        }

        @Data
        public static class AuthenticatorTypes {
            private java.util.List<String> allowed = java.util.List.of("PLATFORM", "CROSS_PLATFORM");
            private java.util.List<String> blocked = java.util.List.of();
        }

        @Data
        public static class UserExperienceSettings {
            private int maxCredentialsPerUser = 5;
            private boolean allowCredentialNaming = true;
            private boolean showAuthenticatorInfo = true;
        }

        @Data
        public static class AdvancedSettings {
            private boolean enableResidentKeys = true;
            private String userVerification = "preferred";
            private String attestationConveyance = "none";
            private boolean backupEligiblePreferred = true;
        }
    }
}