package be.steby.CoreProject.bll.domains.admin.services.useraccount;

import be.steby.CoreProject.bll.common.services.reactivation.ReactivationPolicyService;
import be.steby.CoreProject.bll.domains.admin.events.account.AdminUserActivatedEvent;
import be.steby.CoreProject.bll.domains.admin.events.account.AdminUserDeactivatedEvent;
import be.steby.CoreProject.bll.domains.admin.events.account.AdminUserDeletedEvent;
import be.steby.CoreProject.bll.domains.admin.exceptions.AdminOperationException;
import be.steby.CoreProject.bll.domains.admin.models.account.AdminDeactivationRequest;
import be.steby.CoreProject.bll.domains.admin.models.account.AdminUserCreationRequest;
import be.steby.CoreProject.bll.domains.admin.models.account.AdminUserCreationResult;
import be.steby.CoreProject.bll.domains.admin.models.AdminValidationResult;
import be.steby.CoreProject.bll.domains.admin.services.permissions.AdminPermissionValidator;
import be.steby.CoreProject.bll.domains.admin.services.validation.AdminActionPolicyService;
import be.steby.CoreProject.bll.domains.password.services.tokens.email.PasswordResetTokenServiceImpl;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.bll.common.exceptions.AttributeUnchangedException;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.PasswordResetToken;
import be.steby.CoreProject.dl.enums.ReactivationPolicy;
import be.steby.CoreProject.dl.enums.admin.deactivation.AdminDeactivationCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service implementation for admin operations on user accounts.
 * Handles user lifecycle management: creation, activation, deactivation, reactivation, and deletion.
 *
 * Responsibilities:
 * - Orchestrates user account operations (business logic)
 * - Validates permissions (who can do what)
 * - Publishes domain events (for notifications, audit, etc.)
 * - Delegates technical operations to specialized services
 *
 * Delegation strategy:
 * - AdminUserCreationService → Technical user building/preparation
 * - UserService → Core user persistence and state management
 * - UserPermissionService → Permission validation
 * - AdminPolicyService → Business rule validation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserAccountServiceImpl implements AdminUserAccountService {

    private final UserService userService;
    private final AdminUserCreationService adminUserCreationService;
    private final AdminPermissionValidator adminPermissionValidator;
    private final ReactivationPolicyService reactivationPolicyService;
    private final AdminActionPolicyService adminActionPolicyService;
    private final PasswordResetTokenServiceImpl passwordResetTokenService;
    private final ApplicationEventPublisher eventPublisher;

    // ===============================
    // USER CREATION
    // ===============================

    @Override
    @Transactional
    public User createUser(AdminUserCreationRequest request) {
        log.debug("Admin user creation request - email: {}, roles: {}",
                request.email(), request.userRoles());

        // 1. Get admin actor (for permission checks)
        User actor = userService.getAuthenticatedUser();
        log.debug("User creation initiated by admin: {}", actor.getUsername());

        // 2. Validate request data (email format, names, phone)
        AdminValidationResult validation = adminActionPolicyService.validateUserCreation(request);
        if (!validation.isValid()) {
            String errorMessage = "User creation validation failed: " +
                    String.join(", ", validation.errors());
            log.warn(errorMessage);
            throw new AdminOperationException(errorMessage);
        }
        log.debug("Request data validation passed");

        // 3. Build target user (NOT saved yet) - allows permission checks
        AdminUserCreationResult buildResult = adminUserCreationService.buildUserFromRequest(request);
        User targetNotSaved = buildResult.user();
        String temporaryPassword = buildResult.temporaryPassword();

        log.debug("Built target user {} (not saved) - checking role grant permissions",
                targetNotSaved.getUsername());

//        // 4. Check role granting permissions WITH the built target user
//        for (UserRole role : request.userRole()) {
//            if (!userPermissionService.canGrantRole(actor, targetNotSaved, role)) {
//                log.warn("Admin {} attempted to grant role {} without permission to user {}",
//                        actor.getUsername(), role, targetNotSaved.getUsername());
//
//                // Provide appropriate error message based on role
//                if (role == UserRole.SUPER_ADMIN || role == UserRole.ADMIN) {
//                    throw UserPermissionExceptionFactory.forInsufficientPermissions(
//                            UserRole.SUPER_ADMIN, "grant role " + role);
//                } else {
//                    throw UserPermissionExceptionFactory.forInsufficientPermissions(
//                            UserRole.ADMIN, "grant role " + role);
//                }
//            }
//        }
        log.debug("All role grant permissions validated successfully");

        // 5. Permission checks passed - NOW save the user
        User savedUser = adminUserCreationService.completeUserCreation(
                targetNotSaved,
                temporaryPassword
        );

        log.info("User successfully created by admin - ID: {}, username: {}, created by: {}",
                savedUser.getId(), savedUser.getUsername(), actor.getUsername());

        return savedUser;
    }

    // ===============================
    // USER ACTIVATION
    // ===============================

    @Override
    @Transactional
    public void activateUser(String publicId) {

        // 1. Get actors
        User admin = userService.getAuthenticatedUser();
        User target = userService.getUserByPublicId(publicId);

        log.debug("Admin activation request by adminId: {} - targetId: {}",admin.getId(), target.getId());

        if (target.isEnabled()) {
            throw new AttributeUnchangedException("User is already activated");
        }

        // Admin can reactivate any kind of user
        adminPermissionValidator.validateAdminActionOnAllUsers(admin, target, false, "user-activation");

        // 2. Determine activation type and delegate
        boolean isFirstActivation = !target.isEverActivated();

        if (isFirstActivation) {
            activateUserByAdmin(admin, target);
        } else {
            reactivateUserByAdmin(admin, target);
        }

        userService.saveUser(target);

        // 3. Publish appropriate events
        boolean wasDeactivated = target.getDeactivatedAt() != null;
        AdminUserActivatedEvent event = wasDeactivated
                ? AdminUserActivatedEvent.of(target, admin, true, target.getDeactivatedAt())
                : AdminUserActivatedEvent.simple(target, admin);

        eventPublisher.publishEvent(event);

        String actionType = isFirstActivation ? "activated" : "reactivated";
        log.info("User: {} successfully: {} by admin - ID: {}",
                publicId, actionType, admin.getId());
    }

    // ===============================
    // USER DEACTIVATION
    // ===============================

    @Override
    @Transactional
    public void deactivateUser(String publicId,
                               AdminDeactivationCategory deactivationCategory,
                               String adminDeactivationDetails
        ) {

        // 1. Get actors
        User admin = userService.getAuthenticatedUser();
        User target = userService.getUserByPublicId(publicId);

        log.debug("Admin deactivation request - targetId: {}, category: {}, by admin: {}",
                target.getId(), deactivationCategory, admin.getId());
        if (!target.isEnabled()) {
            throw new AttributeUnchangedException("User is already deactivated");
        }

        // Super admin protection
        if (target.isSuperAdmin()) {
            throw new AdminOperationException("No possibilities to deactivate Super_Admin. Please contact CEO" );
        }

        adminPermissionValidator.validateStrictHierarchy(admin, target, false, "user-deactivation-by-admin");

        // 2. Validate deactivation details via admin policy
        AdminDeactivationRequest deactivationRequest = new AdminDeactivationRequest(
                target.getId(), deactivationCategory, adminDeactivationDetails);

        AdminValidationResult validationResult =
                adminActionPolicyService.validateDeactivationDetails(deactivationRequest);

        if (!validationResult.isValid()) {
            throw new AdminOperationException(
                    "Deactivation validation failed: " +
                            String.join(", ", validationResult.errors()));
        }

        deactivateUserByAdmin(admin, target, deactivationCategory, adminDeactivationDetails);
        userService.saveUser(target);

        // 4. Publish deactivation events
        AdminUserDeactivatedEvent event = AdminUserDeactivatedEvent.simple(
                target, admin, deactivationCategory, adminDeactivationDetails);
        eventPublisher.publishEvent(event);

        log.info("User: {} successfully deactivated by admin: {}, category: {}",
                target.getId(), admin.getId(), deactivationCategory);
    }

    @Override
    @Transactional
    public void deactivateUser(String publicId,
                               AdminDeactivationRequest request) {
        // Delegate to the main method
        deactivateUser(
                publicId,
                request.deactivationCategory(),
                request.adminDeactivationDetails()
        );
    }

    // ===============================
    // USER REACTIVATION
    // ===============================
//
//    @Override
//    @Transactional
//    public void reactivateUser(String publicId) {
//        log.debug("Admin reactivation request - targetId: {}", userId);
//
//        // 1. Get actors
//        User admin = userService.getAuthenticatedUser();
//        User target = userService.getUserById(userId);
//
//        // 2. Delegate to user service
//        userService.adminReactivateUser(target, admin);
//
//        // 3. Publish reactivation events
//        AdminUserActivatedEvent event = AdminUserActivatedEvent.of(
//                target, admin, true, target.getDeactivatedAt());
//        eventPublisher.publishEvent(event);
//
//        log.info("User successfully reactivated by admin - ID: {}, reactivated by: {}",
//                userId, admin.getUsername());
//    }

    // ===============================
// USER DELETION - Updated methods
// ===============================

    @Override
    @Transactional
    public void deleteUser(String publicId) {
        log.debug("Admin delete request - targetId: {}", publicId);

        // Get actors
        User admin = userService.getAuthenticatedUser();
        User target = userService.getUserByPublicId(publicId);

        // Capture user info BEFORE deletion for the event
        AdminUserDeletedEvent event = AdminUserDeletedEvent.hardDelete(target, admin);

        // Delegate to user service (includes permission checks)
        userService.deleteUser(target.getId());

        // Publish event AFTER successful deletion
        eventPublisher.publishEvent(event);

        log.info("User: {} successfully deleted by admin : {}",
                target.getId(), admin.getId());
    }

    @Override
    @Transactional
    public void gdprUserDelete(User user) {
        log.debug("Admin GDPR delete request - user: {}", user.getUsername());

        // Get admin for logging
        User admin = userService.getAuthenticatedUser();

        // Capture user info BEFORE anonymization for the event
        AdminUserDeletedEvent event = AdminUserDeletedEvent.gdprDelete(user, admin);

        // Delegate to user service (includes permission checks and anonymization)
        userService.gdprUserDelete(user);

        // Publish event AFTER successful deletion
        eventPublisher.publishEvent(event);

        log.info("User successfully GDPR deleted by admin - username: {}, deleted by: {}",
                user.getUsername(), admin.getUsername());
    }


    // ===============================
    // PASSWORD MANAGEMENT
    // ===============================

    @Override
    @Transactional
    public void triggerPasswordReset(String publicId) {

        // 1. Get user
        User target = userService.getUserByPublicId(publicId);
        User admin = userService.getAuthenticatedUser();

        log.debug("Admin password reset request - targetId: {}", target.getId());

        // 2. Generate password reset token
        PasswordResetToken token = passwordResetTokenService.createPasswordResetToken(target);

        // 3. Token is automatically sent via email by event listener
        // (PasswordResetTokenCreatedEvent published by createPasswordResetToken)

        log.info("Password reset triggered by admin - targetId: {}, admin: {}, token: {}",
                target.getId(), admin.getId(), token.getPublicId());
    }

    private void activateUserByAdmin(User admin, User target){
        target.setEnabled(true);
        target.setEverActivated(true);
        target.setActivatedAt(Instant.now());
        target.setActivatedBy(admin);
        target.setEmailVerified(true);
    }

    private void reactivateUserByAdmin(User admin, User target){
        //  Determine reactivation policy
        ReactivationPolicy policy = reactivationPolicyService.determineReactivationPolicy(target, admin);

        // 9. Perform reactivation
        target.setEnabled(true);
        target.setReactivatedAt(Instant.now());
        target.setReactivatedBy(admin);
        target.setReactivationPolicy(policy);

        //  Clear deactivation data
        if (target.getDeactivatedAt() != null) {
            target.setDeactivationReason(null);
            target.setDeactivationDetails(null);
            target.setDeactivatedAt(null);
        }

        // Clear admin deactivation data if applicable
        if (target.isAdminDeactivated()) {
            target.setAdminDeactivationReason(null);
            target.setAdminDeactivationDetails(null);
            target.setAdminDeactivatedBy(null);
            target.setAdminDeactivatedAt(null);
        }
    }

    private void deactivateUserByAdmin(User admin, User target, AdminDeactivationCategory deactivationCategory,
                                String adminDeactivationDetails){
        target.setEnabled(false);
        target.setAdminDeactivationReason(deactivationCategory);
        target.setAdminDeactivationDetails(adminDeactivationDetails);
        target.setAdminDeactivatedBy(admin);
        target.setAdminDeactivatedAt(Instant.now());
    }
}