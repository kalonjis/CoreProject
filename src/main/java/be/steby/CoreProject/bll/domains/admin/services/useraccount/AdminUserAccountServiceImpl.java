package be.steby.CoreProject.bll.domains.admin.services.useraccount;

import be.steby.CoreProject.bll.common.exceptions.UserPermissionExceptionFactory;
import be.steby.CoreProject.bll.common.services.permissions.UserPermissionService;
import be.steby.CoreProject.bll.domains.admin.events.account.AdminUserActivatedEvent;
import be.steby.CoreProject.bll.domains.admin.events.account.AdminUserDeactivatedEvent;
import be.steby.CoreProject.bll.domains.admin.exceptions.AdminOperationException;
import be.steby.CoreProject.bll.domains.admin.models.account.AdminDeactivationRequest;
import be.steby.CoreProject.bll.domains.admin.models.account.AdminUserCreationRequest;
import be.steby.CoreProject.bll.domains.admin.models.account.AdminUserCreationResult;
import be.steby.CoreProject.bll.domains.admin.models.AdminValidationResult;
import be.steby.CoreProject.bll.domains.admin.services.validation.AdminPolicyService;
import be.steby.CoreProject.bll.domains.password.services.tokens.PasswordResetTokenServiceImpl;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.PasswordResetToken;
import be.steby.CoreProject.dl.enums.UserRole;
import be.steby.CoreProject.dl.enums.admin.deactivation.AdminDeactivationCategory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final UserPermissionService userPermissionService;
    private final AdminPolicyService adminPolicyService;
    private final PasswordResetTokenServiceImpl passwordResetTokenService;
    private final ApplicationEventPublisher eventPublisher;

    // ===============================
    // USER CREATION
    // ===============================

    @Override
    @Transactional
    public User createUser(AdminUserCreationRequest request, HttpServletRequest httpRequest) {
        log.debug("Admin user creation request - email: {}, roles: {}",
                request.email(), request.userRoles());

        // 1. Get admin actor (for permission checks)
        User actor = userService.getAuthenticatedUser();
        log.debug("User creation initiated by admin: {}", actor.getUsername());

        // 2. Validate request data (email format, names, phone)
        AdminValidationResult validation = adminPolicyService.validateUserCreation(request);
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

        // 4. Check role granting permissions WITH the built target user
        for (UserRole role : request.userRoles()) {
            if (!userPermissionService.canGrantRole(actor, targetNotSaved, role)) {
                log.warn("Admin {} attempted to grant role {} without permission to user {}",
                        actor.getUsername(), role, targetNotSaved.getUsername());

                // Provide appropriate error message based on role
                if (role == UserRole.SUPER_ADMIN || role == UserRole.ADMIN) {
                    throw UserPermissionExceptionFactory.forInsufficientPermissions(
                            UserRole.SUPER_ADMIN, "grant role " + role);
                } else {
                    throw UserPermissionExceptionFactory.forInsufficientPermissions(
                            UserRole.ADMIN, "grant role " + role);
                }
            }
        }
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
    public void activateUser(Long userId, HttpServletRequest request) {
        log.debug("Admin activation request - targetId: {}", userId);

        // 1. Get actors
        User admin = userService.getAuthenticatedUser();
        User target = userService.getUserById(userId);

        // 2. Determine activation type and delegate
        boolean isFirstActivation = !target.isEverActivated();

        if (isFirstActivation) {
            userService.adminActivateUser(target, admin);
        } else {
            userService.adminReactivateUser(target, admin);
        }

        // 3. Publish appropriate events
        boolean wasDeactivated = target.getDeactivatedAt() != null;
        AdminUserActivatedEvent event = wasDeactivated
                ? AdminUserActivatedEvent.of(target, admin, true, target.getDeactivatedAt())
                : AdminUserActivatedEvent.simple(target, admin);

        eventPublisher.publishEvent(event);

        String actionType = isFirstActivation ? "activated" : "reactivated";
        log.info("User successfully {} by admin - ID: {}, {} by: {}",
                actionType, userId, actionType, admin.getUsername());
    }

    // ===============================
    // USER DEACTIVATION
    // ===============================

    @Override
    @Transactional
    public void deactivateUser(Long userId,
                               AdminDeactivationCategory deactivationCategory,
                               String adminDeactivationDetails,
                               HttpServletRequest request) {
        log.debug("Admin deactivation request - targetId: {}, category: {}",
                userId, deactivationCategory);

        // 1. Get actors
        User admin = userService.getAuthenticatedUser();
        User target = userService.getUserById(userId);

        // 2. Validate deactivation details via admin policy
        AdminDeactivationRequest deactivationRequest = new AdminDeactivationRequest(
                userId, deactivationCategory, adminDeactivationDetails);

        AdminValidationResult validationResult =
                adminPolicyService.validateDeactivationDetails(deactivationRequest);

        if (!validationResult.isValid()) {
            throw new AdminOperationException(
                    "Deactivation validation failed: " +
                            String.join(", ", validationResult.errors()));
        }

        // 3. Delegate to user service
        userService.adminDeactivateUser(target, admin, deactivationCategory, adminDeactivationDetails);

        // 4. Publish deactivation events
        AdminUserDeactivatedEvent event = AdminUserDeactivatedEvent.simple(
                target, admin, deactivationCategory, adminDeactivationDetails);
        eventPublisher.publishEvent(event);

        log.info("User successfully deactivated by admin - ID: {}, deactivated by: {}, category: {}",
                userId, admin.getUsername(), deactivationCategory);
    }

    @Override
    @Transactional
    public void deactivateUser(Long userId,
                               AdminDeactivationRequest request,
                               HttpServletRequest httpRequest) {
        // Delegate to the main method
        deactivateUser(
                userId,
                request.deactivationCategory(),
                request.adminDeactivationDetails(),
                httpRequest
        );
    }

    // ===============================
    // USER REACTIVATION
    // ===============================

    @Override
    @Transactional
    public void reactivateUser(Long userId, HttpServletRequest request) {
        log.debug("Admin reactivation request - targetId: {}", userId);

        // 1. Get actors
        User admin = userService.getAuthenticatedUser();
        User target = userService.getUserById(userId);

        // 2. Delegate to user service
        userService.adminReactivateUser(target, admin);

        // 3. Publish reactivation events
        AdminUserActivatedEvent event = AdminUserActivatedEvent.of(
                target, admin, true, target.getDeactivatedAt());
        eventPublisher.publishEvent(event);

        log.info("User successfully reactivated by admin - ID: {}, reactivated by: {}",
                userId, admin.getUsername());
    }

    // ===============================
    // USER DELETION
    // ===============================

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        log.debug("Admin delete request - targetId: {}", userId);

        // Get admin for logging (permission check done in UserService)
        User admin = userService.getAuthenticatedUser();

        // Delegate to user service (includes permission checks)
        userService.deleteUser(userId);

        log.info("User successfully deleted by admin - ID: {}, deleted by: {}",
                userId, admin.getUsername());
    }

    @Override
    @Transactional
    public void gdprUserDelete(User user) {
        log.debug("Admin GDPR delete request - user: {}", user.getUsername());

        // Get admin for logging (permission check done in UserService)
        User admin = userService.getAuthenticatedUser();

        // Delegate to user service (includes permission checks and anonymization)
        userService.gdprUserDelete(user);

        log.info("User successfully GDPR deleted by admin - username: {}, deleted by: {}",
                user.getUsername(), admin.getUsername());
    }

    // ===============================
    // PASSWORD MANAGEMENT
    // ===============================

    @Override
    @Transactional
    public void triggerPasswordReset(Long userId) {
        log.debug("Admin password reset request - targetId: {}", userId);

        // 1. Get user
        User target = userService.getUserById(userId);
        User admin = userService.getAuthenticatedUser();

        // 2. Generate password reset token
        PasswordResetToken token = passwordResetTokenService.createPasswordResetToken(target);

        // 3. Token is automatically sent via email by event listener
        // (PasswordResetTokenCreatedEvent published by createPasswordResetToken)

        log.info("Password reset triggered by admin - targetId: {}, admin: {}, token: {}",
                userId, admin.getUsername(), token.getPublicId());
    }
}