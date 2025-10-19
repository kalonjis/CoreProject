package be.steby.CoreProject.bll.domains.auth.services.twofactor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;

/**
 * Configuration properties for TOTP (Time-based One-Time Password) 2FA.
 *
 * Uses @Value annotations to read from application properties.
 * TOTP-focused, following KISS principle and project conventions.
 *
 * @author Steby Team
 * @since 2.0.0
 */
@Configuration
@Getter
public class TOTPConfiguration {

    @Value("${security.totp.enabled:true}")
    private boolean enabled;

    @Value("${security.totp.issuer-name:Steby CoreProject}")
    private String issuerName;

    @Value("${security.totp.secret-length:20}")
    private int secretLength;

    @Value("${security.totp.code-digits:6}")
    private int codeDigits;

    @Value("${security.totp.time-step-seconds:30}")
    private int timeStepSeconds;

    @Value("${security.totp.window-tolerance:1}")
    private int windowTolerance;

    @Value("${security.totp.hmac-algorithm:HmacSHA1}")
    private String hmacAlgorithm;

    @Value("${security.totp.encryption-key:StebyTOTPKey2024!}")
    private String encryptionKey;
}