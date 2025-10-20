package be.steby.CoreProject.bll.domains.auth.services.twofactor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;

/**
 * Configuration properties for Backup Codes 2FA.
 *
 * Uses @Value annotations to read from application properties.
 * Backup codes focused, following KISS principle and project conventions.
 *
 * @author Steby Team
 * @since 2.0.0
 */
@Configuration
@Getter
public class BackupCodesConfiguration {

    @Value("${security.two-factor.backup-codes.enabled:true}")
    private boolean enabled;

    @Value("${security.two-factor.backup-codes.codes-count:10}")
    private int codesCount;

    @Value("${security.two-factor.backup-codes.code-length:8}")
    private int codeLength;

    @Value("${security.two-factor.backup-codes.single-use:true}")
    private boolean singleUse;

}