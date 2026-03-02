package be.steby.CoreProject.bll.domains.auth.services.twofactor;

import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dal.repositories.TwoFactorAuthRepository;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.TwoFactorAuth;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Helper service qui maintient le flag {@code twoFactorEnabled} sur l'entité
 * {@link User} en cohérence avec les méthodes 2FA actives en base.
 *
 * Règles :
 * - ON  : dès qu'au moins une méthode est activée (hors BACKUP_CODES)
 * - OFF : uniquement si AUCUNE méthode autre que BACKUP_CODES n'est active
 *
 * Doit être appelé APRÈS chaque sauvegarde d'un {@link TwoFactorAuth}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorUserSyncService {

    private final TwoFactorAuthRepository twoFactorAuthRepository;
    private final UserService userService;

    /**
     * Appelé après l'activation d'une méthode 2FA.
     * Met {@code twoFactorEnabled = true} si ce n'est pas déjà le cas.
     */
    public void syncOnEnable(User user) {
        if (!user.isTwoFactorEnabled()) {
            user.setTwoFactorEnabled(true);
            userService.saveUser(user);
            log.info("twoFactorEnabled → true  for user: {}", user.getUsername());
        }
    }

    /**
     * Appelé après la désactivation d'une méthode 2FA.
     * Met {@code twoFactorEnabled = false} seulement s'il ne reste aucune
     * méthode active (hors BACKUP_CODES).
     */
    public void syncOnDisable(User user) {
        long activeRealMethods = twoFactorAuthRepository
                .findByUserAndEnabledTrue(user)
                .stream()
                .filter(m -> m.getType() != TwoFactorType.BACKUP_CODES)
                .count();

        if (activeRealMethods == 0 && user.isTwoFactorEnabled()) {
            user.setTwoFactorEnabled(false);
            userService.saveUser(user);
            log.info("twoFactorEnabled → false for user: {}", user.getUsername());
        }
    }
}