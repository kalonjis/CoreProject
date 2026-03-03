package be.steby.CoreProject.bll.domains.password.services;

import be.steby.CoreProject.bll.domains.password.exceptions.PasswordAlreadyUsedException;
import be.steby.CoreProject.dal.repositories.PasswordHistoryRepository;
import be.steby.CoreProject.dl.entities.PasswordHistory;
import be.steby.CoreProject.dl.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manages the password history for each user.
 *
 * <p>Can be fully disabled via {@code security.password.history.enabled=false}.
 * When disabled, all checks and recordings are no-ops — no DB calls are made.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordHistoryService {

    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${security.password.history.enabled:true}")
    private boolean enabled;

    @Value("${security.password.history.count:5}")
    private int historyCount;

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Checks whether the given plain password has been used recently by the user.
     *
     * <p>Compares the candidate against the last {@code history.count} hashes
     * using BCrypt. Throws {@link PasswordAlreadyUsedException} (409) if matched.
     *
     * <p>No-op if history is disabled.
     *
     * @param user      the user attempting the password change
     * @param plainPassword the plain password to check
     * @throws PasswordAlreadyUsedException if the password was used recently
     */
    public void checkNotRecentlyUsed(User user, String plainPassword) {
        if (!enabled) {
            log.debug("Password history check disabled — skipping");
            return;
        }

        List<PasswordHistory> recent = passwordHistoryRepository.findRecentByUser(user, historyCount);

        boolean alreadyUsed = recent.stream()
                .anyMatch(entry -> passwordEncoder.matches(plainPassword, entry.getPasswordHash()));

        if (alreadyUsed) {
            log.warn("User {} attempted to reuse a recent password", user.getUsername());
            throw new PasswordAlreadyUsedException(historyCount);
        }

        log.debug("Password history check passed for user: {}", user.getUsername());
    }

    /**
     * Records the current (already hashed) password into the user's history,
     * then trims entries beyond the retention limit.
     *
     * <p>Must be called AFTER the password has been saved on the user entity,
     * passing the new hash. No-op if history is disabled.
     *
     * @param user         the user whose history to update
     * @param passwordHash the BCrypt hash of the new password
     */
    @Transactional
    public void record(User user, String passwordHash) {
        if (!enabled) {
            return;
        }

        PasswordHistory entry = PasswordHistory.builder()
                .user(user)
                .passwordHash(passwordHash)
                .build();

        passwordHistoryRepository.save(entry);
        passwordHistoryRepository.deleteOldEntriesBeyondLimit(user, historyCount);

        log.debug("Password history recorded for user: {} ({} entries kept)",
                user.getUsername(), historyCount);
    }
}