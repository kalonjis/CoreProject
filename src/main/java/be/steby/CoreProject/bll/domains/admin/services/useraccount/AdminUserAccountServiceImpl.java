package be.steby.CoreProject.bll.domains.admin.services.useraccount;

import be.steby.CoreProject.bll.common.exceptions.AttributeUnchangedException;
import be.steby.CoreProject.bll.domains.admin.events.account.AdminUserActivatedEvent;
import be.steby.CoreProject.bll.domains.admin.events.account.AdminUserDeactivatedEvent;
import be.steby.CoreProject.bll.domains.admin.events.account.AdminUserDeletedEvent;
import be.steby.CoreProject.bll.domains.admin.exceptions.AdminOperationException;
import be.steby.CoreProject.bll.domains.admin.models.AdminValidationResult;
import be.steby.CoreProject.bll.domains.admin.models.account.AdminDeactivationRequest;
import be.steby.CoreProject.bll.domains.admin.models.account.AdminUserCreationRequest;
import be.steby.CoreProject.bll.domains.admin.models.account.AdminUserCreationResult;
import be.steby.CoreProject.bll.domains.admin.services.permissions.AdminPermissionValidator;
import be.steby.CoreProject.bll.domains.admin.services.validation.AdminActionPolicyService;
import be.steby.CoreProject.bll.domains.user.exceptions.UserAlreadyActivatedException;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.admin.deactivation.AdminDeactivationCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link AdminUserAccountService}.
 *
 * <h3>Responsibilities — orchestration only</h3>
 * <ol>
 *   <li>Resolve actors (admin + target) from the security context</li>
 *   <li>Validate permissions via {@link AdminPermissionValidator}</li>
 *   <li>Validate business rules via {@link AdminActionPolicyService}</li>
 *   <li>Delegate all state mutations to {@link UserService}</li>
 *   <li>Publish domain events for audit and notifications</li>
 * </ol>
 *
 * <p><strong>This class never calls setters on a User entity.</strong>
 * Field-level mutations are the exclusive responsibility of {@link UserService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserAccountServiceImpl implements AdminUserAccountService {

    private final UserService userService;
    private final AdminUserCreationService adminUserCreationService;
    private final AdminPermissionValidator adminPermissionValidator;
    private final AdminActionPolicyService adminActionPolicyService;
    private final ApplicationEventPublisher eventPublisher;

    // =========================================================================
    // USER CREATION
    // =========================================================================

    @Override
    @Transactional
    public User createUser(AdminUserCreationRequest request) {
        log.debug("Admin user creation request — email: {}, roles: {}",
                request.email(), request.userRoles());

        // 1. Resolve actor
        User admin = userService.getAuthenticatedUser();

        // 2. Validate request payload (email format, names, phone)
        AdminValidationResult validation = adminActionPolicyService.validateUserCreation(request);
        if (!validation.isValid()) {
            String msg = "User creation validation failed: " + String.join(", ", validation.errors());
            log.warn(msg);
            throw new AdminOperationException(msg);
        }

        // 3. Build user entity (NOT saved yet) — needed for permission checks on assigned roles
        AdminUserCreationResult buildResult = adminUserCreationService.buildUserFromRequest(request);
        User targetNotSaved = buildResult.user();
        String temporaryPassword = buildResult.temporaryPassword();

        log.debug("Target user built (not saved) — checking role grant permissions for {}",
                targetNotSaved.getUsername());

        // 4. Validate role-grant permissions
        adminPermissionValidator.validateStrictHierarchy(admin, targetNotSaved, false, "user-creation");

        // 5. Permissions passed — persist and publish
        User saved = adminUserCreationService.completeUserCreation(targetNotSaved, temporaryPassword);

        log.info("User {} created by admin {}", saved.getUsername(), admin.getUsername());
        return saved;
    }

    // =========================================================================
    // USER ACTIVATION  (first-time — everActivated == false)
    // =========================================================================

    @Override
    @Transactional
    public void activateUser(String publicId) {
        // 1. Resolve actors
        User admin  = userService.getAuthenticatedUser();
        User target = userService.getUserByPublicId(publicId);

        log.debug("Admin activation request — admin: {}, target: {}",
                admin.getUsername(), target.getUsername());

        // 2. Guard: must never have been activated before
        if (target.isEverActivated()) {
            throw new UserAlreadyActivatedException(
                    "User " + target.getUsername() + " was already activated — use reactivateUser instead"
            );
        }

        // 3. Permission check
        adminPermissionValidator.validateStrictHierarchy(admin, target, false, "user-activation");

        // 4. Delegate mutation to UserService
        userService.adminActivateUser(target, admin);

        // 5. Publish event
        eventPublisher.publishEvent(AdminUserActivatedEvent.simple(target, admin));

        log.info("User {} activated by admin {}", target.getUsername(), admin.getUsername());
    }

    // =========================================================================
    // USER REACTIVATION  (everActivated == true && enabled == false)
    // =========================================================================

    @Override
    @Transactional
    public void reactivateUser(String publicId) {
        // 1. Resolve actors
        User admin  = userService.getAuthenticatedUser();
        User target = userService.getUserByPublicId(publicId);

        log.debug("Admin reactivation request — admin: {}, target: {}",
                admin.getUsername(), target.getUsername());

        // 2. Guard: must have been activated before
        if (!target.isEverActivated()) {
            throw new IllegalStateException(
                    "User " + target.getUsername() + " was never activated — use activateUser instead"
            );
        }

        // 3. Guard: must currently be disabled
        if (target.isEnabled()) {
            throw new AttributeUnchangedException("User is already active");
        }

        // 4. Permission check
        adminPermissionValidator.validateStrictHierarchy(admin, target, false, "user-reactivation");

        // 5. Delegate mutation to UserService (includes policy eligibility check)
        userService.adminReactivateUser(target, admin);

        // 6. Publish event
        eventPublisher.publishEvent(
                AdminUserActivatedEvent.of(target, admin, true, target.getAdminDeactivatedAt())
        );

        log.info("User {} reactivated by admin {}", target.getUsername(), admin.getUsername());
    }

    // =========================================================================
    // USER DEACTIVATION
    // =========================================================================

    @Override
    @Transactional
    public void deactivateUser(String publicId, AdminDeactivationCategory category, String details) {
        // 1. Resolve actors
        User admin  = userService.getAuthenticatedUser();
        User target = userService.getUserByPublicId(publicId);

        log.debug("Admin deactivation request — admin: {}, target: {}, category: {}",
                admin.getUsername(), target.getUsername(), category);

        // 2. Guard: cannot deactivate a SUPER_ADMIN
        if (target.isSuperAdmin()) {
            throw new AdminOperationException(
                    "Cannot deactivate a SUPER_ADMIN account. Please contact the CEO."
            );
        }

        // 3. Permission check (strict hierarchy)
        adminPermissionValidator.validateStrictHierarchy(admin, target, false, "user-deactivation");

        // 4. Validate deactivation payload (category + details)
        AdminDeactivationRequest deactivationRequest =
                new AdminDeactivationRequest(target.getId(), category, details);
        AdminValidationResult validation =
                adminActionPolicyService.validateDeactivationDetails(deactivationRequest);
        if (!validation.isValid()) {
            throw new AdminOperationException(
                    "Deactivation validation failed: " + String.join(", ", validation.errors())
            );
        }

        // 5. Delegate mutation to UserService
        userService.adminDeactivateUser(target, admin, category, details);

        // 6. Publish event
        eventPublisher.publishEvent(
                AdminUserDeactivatedEvent.simple(target, admin, category, details)
        );

        log.info("User {} deactivated by admin {} — category: {}",
                target.getUsername(), admin.getUsername(), category);
    }

    @Override
    @Transactional
    public void deactivateUser(String publicId, AdminDeactivationRequest request) {
        deactivateUser(publicId, request.deactivationCategory(), request.adminDeactivationDetails());
    }

    // =========================================================================
    // USER DELETION
    // =========================================================================

    @Override
    @Transactional
    public void deleteUser(String publicId) {
        // 1. Resolve actors
        User admin  = userService.getAuthenticatedUser();
        User target = userService.getUserByPublicId(publicId);

        log.debug("Admin hard-delete request — admin: {}, target: {}",
                admin.getUsername(), target.getUsername());

        // 2. Only SUPER_ADMIN can hard-delete
        adminPermissionValidator.validateSuperAdminRole(admin);

        // 3. Capture identifying info BEFORE deletion (entity will be gone after)
        AdminUserDeletedEvent event = AdminUserDeletedEvent.hardDelete(target, admin);

        // 4. Delegate deletion to UserService
        userService.deleteUser(target.getId());

        // 5. Publish event AFTER successful deletion
        eventPublisher.publishEvent(event);

        log.info("User {} permanently deleted by admin {}", target.getUsername(), admin.getUsername());
    }

    @Override
    @Transactional
    public void gdprDeleteUser(String publicId) {
        // 1. Resolve actors
        User admin  = userService.getAuthenticatedUser();
        User target = userService.getUserByPublicId(publicId);

        log.debug("Admin GDPR deletion request — admin: {}, target: {}",
                admin.getUsername(), target.getUsername());

        // 2. Only SUPER_ADMIN can trigger GDPR deletion
        adminPermissionValidator.validateSuperAdminRole(admin);

        // 3. Capture identifying info BEFORE anonymization (PII will be wiped)
        AdminUserDeletedEvent event = AdminUserDeletedEvent.gdprDelete(target, admin);

        // 4. Delegate anonymization to UserService
        userService.anonymizeUser(target);

        // 5. Publish event AFTER successful anonymization
        eventPublisher.publishEvent(event);

        log.info("User {} GDPR-anonymized by admin {}", target.getUsername(), admin.getUsername());
    }
}