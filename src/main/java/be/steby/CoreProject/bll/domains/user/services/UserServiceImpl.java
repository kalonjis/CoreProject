package be.steby.CoreProject.bll.domains.user.services;

import be.steby.CoreProject.bll.common.exceptions.AttributeUnchangedException;
import be.steby.CoreProject.bll.common.exceptions.UserPermissionException;
import be.steby.CoreProject.bll.common.exceptions.UsernameAlreadyTakenException;
import be.steby.CoreProject.bll.common.models.reactivation.ReactivationEligibility;
import be.steby.CoreProject.bll.common.services.reactivation.ReactivationPolicyService;
import be.steby.CoreProject.bll.domains.emailaddress.exceptions.EmailAlreadyUsedException;
import be.steby.CoreProject.bll.domains.user.events.UserPersistedEvent;
import be.steby.CoreProject.bll.domains.user.exceptions.UserNotFoundException;
import be.steby.CoreProject.dal.repositories.UserRepository;
import be.steby.CoreProject.dal.specifications.UserSpecification;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.DeactivationReason;
import be.steby.CoreProject.dl.enums.ReactivationPolicy;
import be.steby.CoreProject.dl.enums.UserRole;
import be.steby.CoreProject.dl.enums.admin.deactivation.AdminDeactivationCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Core implementation of {@link UserService}.
 *
 * <p>This class is the <strong>sole owner of user state mutations</strong>.
 * All field-level changes on a {@link User} entity go through this service.
 * No other service should call setters on a User directly.
 *
 * <h3>Delegation</h3>
 * <ul>
 *   <li>Authentication context → {@link UserAuthenticationService} (cache-first)</li>
 *   <li>Reactivation eligibility → {@link ReactivationPolicyService}</li>
 *   <li>Cache invalidation → {@link UserPersistedEvent} (published on every save)</li>
 * </ul>
 *
 * <h3>Permission model</h3>
 * <p>This service carries <strong>no permission guards</strong>.
 * Callers (AccountService, AdminUserAccountService) are solely responsible
 * for validating access before invoking any method here.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ReactivationPolicyService reactivationPolicyService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserAuthenticationService userAuthenticationService;

    // =========================================================================
    // SEARCH & QUERY
    // =========================================================================

    @Override
    public Page<User> searchUsers(String query, Pageable pageable) {
        return userRepository.findAll(UserSpecification.searchInAllFields(query), pageable);
    }

    @Override
    public Page<User> searchUsersByCriteria(String username, String firstname, String lastname,
                                            String email, String phoneNumber, Pageable pageable) {
        return userRepository.findAll(
                UserSpecification.searchByCriteria(username, firstname, lastname, email, phoneNumber),
                pageable
        );
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));
    }

    @Override
    public User getUserByPublicId(String publicId) {
        return userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new UserNotFoundException("User not found with publicId: " + publicId));
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }

    @Override
    public User getUserByUsernameOrByEmail(String identifier) {
        return userRepository.findByUsernameOrEmail(identifier)
                .orElseThrow(() -> new UserNotFoundException("User not found with identifier: " + identifier));
    }

    @Override
    public Long getTotalUsers() {
        return userRepository.count();
    }

    // =========================================================================
    // PERSISTENCE
    // =========================================================================

    /**
     * Persists a user and publishes {@link UserPersistedEvent} for cache invalidation.
     * This is the only save path — direct repository calls are forbidden.
     */
    @Override
    public User saveUser(User user) {
        User saved = userRepository.save(user);
        eventPublisher.publishEvent(new UserPersistedEvent(saved));
        log.debug("User {} saved — UserPersistedEvent published", saved.getUsername());
        return saved;
    }

    // =========================================================================
    // SELF — Activation
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Mutated fields: {@code enabled}, {@code everActivated}, {@code activatedAt}.
     */
    @Override
    @Transactional
    public void activateUser(Long id) {
        User user = getUserById(id);

        if (user.isEnabled()) {
            throw new AttributeUnchangedException("User is already activated");
        }

        user.setEnabled(true);
        user.setEverActivated(true);
        user.setActivatedAt(Instant.now());

        saveUser(user);
        log.info("User {} self-activated", user.getUsername());
    }

    // =========================================================================
    // SELF — Deactivation
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Mutated fields: {@code enabled}, {@code deactivatedAt},
     * {@code deactivationReason}, {@code deactivationDetails}.
     */
    @Override
    @Transactional
    public void deactivateUser(Long id, DeactivationReason reason, String reasonDetails) {
        User user = getUserById(id);

        if (!user.isEnabled()) {
            throw new AttributeUnchangedException("User is already deactivated");
        }

        user.setEnabled(false);
        user.setDeactivatedAt(Instant.now());
        user.setDeactivationReason(reason);
        user.setDeactivationDetails(reasonDetails);

        saveUser(user);
        log.info("User {} self-deactivated with reason: {}", user.getUsername(), reason);
    }

    // =========================================================================
    // SELF — Reactivation
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Mutated fields: {@code enabled}, {@code reactivatedAt}, {@code reactivatedBy},
     * {@code reactivationPolicy}. Clears: {@code deactivationReason},
     * {@code deactivationDetails}, {@code deactivatedAt}.
     */
    @Override
    @Transactional
    public void reactivateUser(User user) {
        if (user.isEnabled()) {
            throw new AttributeUnchangedException("User is already activated");
        }

        ReactivationEligibility eligibility = reactivationPolicyService.checkEligibility(user, user);
        if (!eligibility.isEligible()) {
            throw new UserPermissionException("Reactivation not allowed: " + eligibility.getReason());
        }

        ReactivationPolicy policy = reactivationPolicyService.determineReactivationPolicy(user, user);

        user.setEnabled(true);
        user.setReactivatedAt(Instant.now());
        user.setReactivatedBy(user);
        user.setReactivationPolicy(policy);
        clearSelfDeactivationFields(user);

        saveUser(user);
        log.info("User {} successfully self-reactivated", user.getUsername());
    }

    // =========================================================================
    // SELF — GDPR Deletion
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Mutated fields: {@code email}, {@code firstname}, {@code lastname},
     * {@code phoneNumber}, {@code enabled}, {@code gdprDeletedAt}.
     */
    @Override
    @Transactional
    public void anonymizeUser(User user) {
        user.setEmail("deleted_" + user.getId() + "@anonymized.local");
        user.setFirstname("User");
        user.setLastname("Deleted");
        user.setPhoneNumber(null);
        user.setEnabled(false);
        user.setGdprDeletedAt(Instant.now());

        saveUser(user);
        log.info("User {} anonymized (GDPR)", user.getUsername());
    }

    // =========================================================================
    // ADMIN — Activation (first-time only)
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Mutated fields: {@code enabled}, {@code everActivated}, {@code activatedAt},
     * {@code activatedBy}, {@code emailVerified}.
     */
    @Override
    @Transactional
    public void adminActivateUser(User target, User admin) {
        if (target.isEnabled()) {
            throw new AttributeUnchangedException("User is already activated");
        }
        if (target.isEverActivated()) {
            throw new IllegalStateException(
                    "User " + target.getUsername() + " was already activated before — use adminReactivateUser instead"
            );
        }

        target.setEnabled(true);
        target.setEverActivated(true);
        target.setActivatedAt(Instant.now());
        target.setActivatedBy(admin);
        target.setEmailVerified(true);

        saveUser(target);
        log.info("User {} activated by admin {}", target.getUsername(), admin.getUsername());
    }

    // =========================================================================
    // ADMIN — Deactivation
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Mutated fields: {@code enabled}, {@code adminDeactivatedAt},
     * {@code adminDeactivationReason}, {@code adminDeactivationDetails},
     * {@code adminDeactivatedBy}.
     */
    @Override
    @Transactional
    public void adminDeactivateUser(User target, User admin,
                                    AdminDeactivationCategory category,
                                    String details) {
        if (!target.isEnabled()) {
            throw new AttributeUnchangedException("User is already deactivated");
        }

        target.setEnabled(false);
        target.setAdminDeactivatedAt(Instant.now());
        target.setAdminDeactivationReason(category);
        target.setAdminDeactivationDetails(details);
        target.setAdminDeactivatedBy(admin);

        saveUser(target);
        log.info("User {} deactivated by admin {} — category: {}",
                target.getUsername(), admin.getUsername(), category);
    }

    // =========================================================================
    // ADMIN — Reactivation
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Mutated fields: {@code enabled}, {@code reactivatedAt}, {@code reactivatedBy},
     * {@code reactivationPolicy}. Clears: self-deactivation fields always,
     * admin-deactivation fields if applicable.
     */
    @Override
    @Transactional
    public void adminReactivateUser(User target, User admin) {
        if (target.isEnabled()) {
            throw new AttributeUnchangedException("User is already activated");
        }
        if (!target.isEverActivated()) {
            throw new IllegalStateException(
                    "User " + target.getUsername() + " was never activated — use adminActivateUser instead"
            );
        }

        ReactivationEligibility eligibility = reactivationPolicyService.checkEligibility(target, admin);
        if (!eligibility.isEligible()) {
            throw new UserPermissionException("Reactivation not allowed: " + eligibility.getReason());
        }

        ReactivationPolicy policy = reactivationPolicyService.determineReactivationPolicy(target, admin);

        target.setEnabled(true);
        target.setReactivatedAt(Instant.now());
        target.setReactivatedBy(admin);
        target.setReactivationPolicy(policy);

        clearSelfDeactivationFields(target);
        clearAdminDeactivationFields(target);

        saveUser(target);
        log.info("User {} reactivated by admin {}", target.getUsername(), admin.getUsername());
    }

    // =========================================================================
    // ADMIN — Hard Deletion
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Irreversible. No permission guard here — AdminUserAccountService is responsible.
     */
    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);
        userRepository.delete(user);
        log.info("User {} permanently deleted", user.getUsername());
    }

    // =========================================================================
    // VALIDATION & CHECKS
    // =========================================================================

    @Override
    public void setUserMailVerified(User user) {
        user.setEmailVerified(true);
        saveUser(user);
    }

    @Override
    public void checkIfUserExists(User user) {
        if (existsByUsername(user.getUsername())) {
            throw new UsernameAlreadyTakenException(
                    "User account with username: " + user.getUsername() + " already exists");
        }
        if (existsByEmail(user.getEmail())) {
            throw new EmailAlreadyUsedException(
                    "User account with email: " + user.getEmail() + " already exists");
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsernameIgnoreCase(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailIgnoreCase(email);
    }

    // =========================================================================
    // AUTHENTICATION & SECURITY
    // =========================================================================

    @Override
    public User getAuthenticatedUser() {
        return userAuthenticationService.getSecureAuthenticatedUser();
    }

    @Override
    public boolean isAnonymous() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null || !auth.isAuthenticated()
                || auth.getPrincipal().equals("anonymousUser");
    }

    @Override
    public boolean authenticatedHasRole(UserRole role) {
        return SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(role.name()));
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Clears all self-deactivation fields on the given user.
     * Called on both self-reactivation and admin-reactivation.
     */
    private void clearSelfDeactivationFields(User user) {
        user.setDeactivationReason(null);
        user.setDeactivationDetails(null);
        user.setDeactivatedAt(null);
    }

    /**
     * Clears all admin-deactivation fields on the given user.
     * Called only on admin-reactivation when the account was admin-deactivated.
     */
    private void clearAdminDeactivationFields(User user) {
        if (user.isAdminDeactivated()) {
            user.setAdminDeactivationReason(null);
            user.setAdminDeactivationDetails(null);
            user.setAdminDeactivatedAt(null);
            user.setAdminDeactivatedBy(null);
        }
    }
}