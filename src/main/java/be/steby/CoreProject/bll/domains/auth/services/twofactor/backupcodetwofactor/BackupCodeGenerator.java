package be.steby.CoreProject.bll.domains.auth.services.twofactor.backupcodetwofactor;

import be.steby.CoreProject.bll.domains.auth.services.twofactor.config.BackupCodeConfiguration;
import be.steby.CoreProject.dl.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for generating secure backup codes.
 *
 * Generates cryptographically secure random backup codes using configurable
 * length and character sets. Uses SecureRandom for proper entropy.
 *
 * @author Steby Team
 * @since 2.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BackupCodeGenerator {

    private final BackupCodeConfiguration config;
    private final SecureRandom secureRandom = new SecureRandom();

    // Safe alphanumeric charset (excludes ambiguous characters: 0, O, 1, I, l)
    private static final String ALPHANUMERIC_SAFE = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";

    /**
     * Generate backup codes for a user.
     *
     * @param user the user for whom to generate codes
     * @param length the length of each code (overrides config if provided)
     * @return list of plain text backup codes
     */
    public List<String> generateCodes(User user, int length) {
        log.debug("Generating {} backup codes of length {} for user: {}",
                config.getCount(), length, user.getUsername());

        List<String> codes = new ArrayList<>();
        String charset = getCharset();

        for (int i = 0; i < config.getCount(); i++) {
            String code = generateSingleCode(charset, length);
            codes.add(code);
        }

        log.debug("Generated {} backup codes for user: {}", codes.size(), user.getUsername());
        return codes;
    }

    /**
     * Generate backup codes for a user using configured length.
     *
     * @param user the user for whom to generate codes
     * @return list of plain text backup codes
     */
    public List<String> generateCodes(User user) {
        return generateCodes(user, config.getLength());
    }

    /**
     * Generate a single backup code with formatting.
     * Format: XXXX-XXXX-XXXX-XXXX (4-character blocks separated by dashes)
     *
     * @param charset the character set to use
     * @param length the length of the code (before formatting)
     * @return a single formatted backup code
     */
    private String generateSingleCode(String charset, int length) {
        StringBuilder code = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int randomIndex = secureRandom.nextInt(charset.length());
            code.append(charset.charAt(randomIndex));
        }

        // Format in 4-character blocks with dashes (like TemporaryPasswordGenerator)
        return formatIntoBlocks(code.toString(), 4);
    }

    /**
     * Format a backup code into blocks separated by dashes.
     * Example: "A2B4C6D8E9F2G4H6" -> "A2B4-C6D8-E9F2-G4H6"
     *
     * @param code the raw code to format
     * @param blockSize the size of each block
     * @return formatted code with dashes
     */
    private String formatIntoBlocks(String code, int blockSize) {
        StringBuilder formatted = new StringBuilder();

        for (int i = 0; i < code.length(); i++) {
            if (i > 0 && i % blockSize == 0) {
                formatted.append('-');
            }
            formatted.append(code.charAt(i));
        }

        return formatted.toString();
    }

    /**
     * Get the character set based on configuration.
     *
     * @return the character set string
     */
    private String getCharset() {
        return switch (config.getCharset().toUpperCase()) {
            case "ALPHANUMERIC_SAFE" -> ALPHANUMERIC_SAFE;
            default -> {
                log.warn("Unknown charset: {}, using ALPHANUMERIC_SAFE", config.getCharset());
                yield ALPHANUMERIC_SAFE;
            }
        };
    }
}