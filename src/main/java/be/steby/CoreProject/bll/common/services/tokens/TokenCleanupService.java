package be.steby.CoreProject.bll.common.services.tokens;

import be.steby.CoreProject.dal.repositories.tokens.BaseTokenRepository;
import be.steby.CoreProject.dl.entities.tokens.BaseToken;
import be.steby.CoreProject.dl.entities.tokens.enums.TokenType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Unified token cleanup service that handles expired and revoked tokens
 * across all token types. This service replaces individual cleanup tasks
 * in each token service, providing centralized and efficient cleanup.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupService {

    private final BaseTokenRepository<BaseToken> baseTokenRepository;

    /**
     * Unified scheduled task to clean up expired and revoked tokens.
     * Runs every hour and processes all token types in a single operation.
     *
     * This replaces the individual @Scheduled methods in:
     * - RefreshTokenServiceImpl
     * - AccountConfirmationTokenServiceImpl
     * - PasswordResetTokenServiceImpl
     * - EmailConfirmationTokenServiceImpl
     * - DeviceConfirmationTokenServiceImpl
     * - AccountDeactivationTokenServiceImpl
     * - AccountReactivationTokenServiceImpl
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour at minute 0
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting unified cleanup of expired and revoked tokens");

        Instant now = Instant.now();
        Map<TokenType, Integer> cleanupStats = new HashMap<>();
        int totalDeleted = 0;

        // Clean up each token type and collect statistics
        for (TokenType tokenType : TokenType.values()) {
            try {
                int deletedCount = baseTokenRepository.deleteExpiredTokensByType(tokenType, now);
                cleanupStats.put(tokenType, deletedCount);
                totalDeleted += deletedCount;

                if (deletedCount > 0) {
                    log.info("Deleted {} expired/revoked {} tokens", deletedCount, tokenType.getDescription());
                }
            } catch (Exception e) {
                log.error("Error cleaning up {} tokens: {}", tokenType.getDescription(), e.getMessage());
            }
        }

        // Summary logging
        if (totalDeleted > 0) {
            log.info("Cleanup completed: {} total tokens deleted across {} types",
                    totalDeleted, cleanupStats.size());

            // Detailed breakdown for monitoring
            cleanupStats.entrySet().stream()
                    .filter(entry -> entry.getValue() > 0)
                    .forEach(entry ->
                            log.debug("  - {}: {} tokens deleted",
                                    entry.getKey().getDescription(), entry.getValue())
                    );
        } else {
            log.debug("Cleanup completed: no expired tokens found");
        }
    }

    /**
     * Manual cleanup method that can be called programmatically.
     * Useful for testing or administrative operations.
     *
     * @return Map containing cleanup statistics by token type
     */
    @Transactional
    public Map<TokenType, Integer> performManualCleanup() {
        log.info("Performing manual token cleanup");

        Instant now = Instant.now();
        Map<TokenType, Integer> stats = new HashMap<>();

        for (TokenType tokenType : TokenType.values()) {
            int deleted = baseTokenRepository.deleteExpiredTokensByType(tokenType, now);
            stats.put(tokenType, deleted);
        }

        int total = stats.values().stream().mapToInt(Integer::intValue).sum();
        log.info("Manual cleanup completed: {} tokens deleted", total);

        return stats;
    }

    /**
     * Cleanup specific token type only.
     * Useful for targeted cleanup operations.
     *
     * @param tokenType the specific token type to clean up
     * @return number of tokens deleted
     */
    @Transactional
    public int cleanupTokenType(TokenType tokenType) {
        log.info("Cleaning up {} tokens", tokenType.getDescription());

        int deleted = baseTokenRepository.deleteExpiredTokensByType(tokenType, Instant.now());

        if (deleted > 0) {
            log.info("Deleted {} {} tokens", deleted, tokenType.getDescription());
        }

        return deleted;
    }

    /**
     * Get cleanup statistics without performing cleanup.
     * Useful for monitoring and dashboards.
     *
     * @return Map containing count of expired tokens by type
     */
    @Transactional(readOnly = true)
    public Map<TokenType, Long> getCleanupStatistics() {
        Instant now = Instant.now();
        Map<TokenType, Long> stats = new HashMap<>();

        for (TokenType tokenType : TokenType.values()) {
            // Count tokens that would be deleted (expired or revoked)
            long count = baseTokenRepository.countExpiredTokensByType(tokenType, now);
            stats.put(tokenType, count);
        }

        return stats;
    }
}