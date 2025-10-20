package be.steby.CoreProject.bll.domains.auth.services.twofactor.backupcodes;

import be.steby.CoreProject.bll.domains.auth.services.twofactor.config.BackupCodesConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * Generates secure backup codes for two-factor authentication.
 * 
 * @author Steby Team
 * @since 2.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BackupCodesPasswordGenerator {

    private final BackupCodesConfiguration config;
    private final SecureRandom secureRandom = new SecureRandom();
    
    // Alphanumeric without ambiguous characters (0, O, 1, I, L)
    private static final String ALLOWED_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    /**
     * Generate a set of unique backup codes.
     */
    public List<String> generateBackupCodes() {
        log.debug("Generating {} backup codes with length {}", config.getCodesCount(), config.getCodeLength());
        
        Set<String> uniqueCodes = new HashSet<>();
        List<String> codes = new ArrayList<>();
        
        while (uniqueCodes.size() < config.getCodesCount()) {
            String code = generateSingleCode();
            if (uniqueCodes.add(code)) {
                codes.add(formatCode(code));
            }
        }
        
        log.info("Successfully generated {} backup codes", codes.size());
        return codes;
    }

    /**
     * Generate a single backup code.
     */
    private String generateSingleCode() {
        StringBuilder code = new StringBuilder(config.getCodeLength());
        
        for (int i = 0; i < config.getCodeLength(); i++) {
            int randomIndex = secureRandom.nextInt(ALLOWED_CHARS.length());
            code.append(ALLOWED_CHARS.charAt(randomIndex));
        }
        
        return code.toString();
    }

    /**
     * Format code with separator every 4 characters (e.g., "ABCD-EFGH-1234-5678").
     */
    private String formatCode(String code) {
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < code.length(); i++) {
            if (i > 0 && i % 4 == 0) {
                formatted.append("-");
            }
            formatted.append(code.charAt(i));
        }
        return formatted.toString();
    }
}