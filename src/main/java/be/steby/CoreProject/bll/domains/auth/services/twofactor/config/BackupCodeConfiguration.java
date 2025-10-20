package be.steby.CoreProject.bll.domains.auth.services.twofactor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for backup codes 2FA.
 * Maps values from 2fa-backup-codes.yml configuration file.
 * 
 * @author Steby Team
 * @since 2.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "security.two-factor-auth.backup-codes")
public class BackupCodeConfiguration {
    
    /**
     * Number of backup codes to generate per user.
     * Default: 10
     */
    private int count = 10;
    
    /**
     * Length of each backup code in characters.
     * Default: 16
     */
    private int length = 16;
    
    /**
     * Character set for code generation.
     * Default: ALPHANUMERIC_SAFE
     */
    private String charset = "ALPHANUMERIC_SAFE";
    
    /**
     * Security-related configuration.
     */
    private Security security = new Security();
    
    @Data
    public static class Security {
        
        /**
         * Maximum failed attempts before lockout.
         * Default: 3
         */
        private int maxFailedAttempts = 3;
        
        /**
         * Lockout duration in minutes.
         * Default: 15
         */
        private int lockoutDurationMinutes = 15;
    }
}