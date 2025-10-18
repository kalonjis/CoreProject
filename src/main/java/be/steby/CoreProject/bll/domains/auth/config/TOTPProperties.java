package be.steby.CoreProject.bll.domains.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for TOTP (Time-based One-Time Password) functionality.
 * 
 * Maps automatically from app.security.totp.* in application.yml
 *
 * @author Steby Team
 * @since 2.0.0
 */
@Component
@ConfigurationProperties(prefix = "app.security.totp")
@Data
public class TOTPProperties {
    
    /**
     * Issuer name displayed in authenticator apps
     * Maps from: app.security.totp.issuer
     */
    private String issuer;
    
    /**
     * Time step in seconds for TOTP generation (standard = 30)
     * Maps from: app.security.totp.time-step
     */
    private int timeStep;
    
    /**
     * Time skew tolerance - nombre de périodes acceptées (±1 = ±30 sec)
     * Maps from: app.security.totp.time-skew-tolerance
     */
    private int timeSkewTolerance;
    
    /**
     * Number of digits in generated codes (standard = 6)
     * Maps from: app.security.totp.code-digits
     */
    private int codeDigits;
    
    /**
     * Hash algorithm for TOTP (SHA1 = plus compatible)
     * Maps from: app.security.totp.algorithm
     */
    private String algorithm;
    
    /**
     * Secret length in bits (160 bits = 32 caractères base32)
     * Maps from: app.security.totp.secret-length
     */
    private int secretLength;
    
    /**
     * Configuration spécifique QR Code TOTP
     * Maps from: app.security.totp.qr-code.*
     */
    private QRCodeConfig qrCode = new QRCodeConfig();
    
    @Data
    public static class QRCodeConfig {
        /**
         * QR Code size in pixels pour TOTP
         * Maps from: app.security.totp.qr-code.size
         */
        private int size = 200;
        
        /**
         * QR Code image format pour TOTP  
         * Maps from: app.security.totp.qr-code.format
         */
        private String format = "PNG";
    }
}