package be.steby.CoreProject.bll.domains.admin.services;

import be.steby.CoreProject.bll.common.exceptions.UserPermissionExceptionFactory;
import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.bll.common.models.user.UserCreationRequest;
import be.steby.CoreProject.bll.common.models.user.UserCreationResult;
import be.steby.CoreProject.bll.common.services.context.RequestContextService;
import be.steby.CoreProject.bll.common.services.permissions.UserPermissionService;
import be.steby.CoreProject.bll.common.services.user.UserCreationService;
import be.steby.CoreProject.bll.common.services.mailer.MailerService;
import be.steby.CoreProject.bll.domains.admin.events.AdminUserActivatedEvent;
import be.steby.CoreProject.bll.domains.admin.events.AdminUserDeactivatedEvent;
import be.steby.CoreProject.bll.domains.admin.exceptions.AdminOperationException;
import be.steby.CoreProject.bll.domains.admin.models.AdminDeactivationRequest;
import be.steby.CoreProject.bll.domains.admin.models.AdminUserCreationRequest;
import be.steby.CoreProject.bll.domains.admin.models.AdminValidationResult;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.bll.domains.password.services.tokens.PasswordResetTokenServiceImpl;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.tokens.PasswordResetToken;
import be.steby.CoreProject.dl.enums.admin.deactivation.AdminDeactivationCategory;
import be.steby.CoreProject.dl.enums.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Admin service implementation focused on orchestration and events publishing.
 * Delegates all business logic to UserService while handling admin-specific concerns.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final UserService userService;
    private final DeviceService deviceService;
    private final MailerService mailerService;
    private final PasswordResetTokenServiceImpl passwordResetTokenService;
    private final RequestContextService requestContextService;
    private final UserCreationService userCreationService;
    private final UserPermissionService userPermissionService;
    private final AdminPolicyService adminPolicyService;
    private final ApplicationEventPublisher eventPublisher;

    // ===============================
    // USER MANAGEMENT
    // ===============================

    @Override
    @Transactional
    public User createUser(User user, HttpServletRequest request) {
        log.debug("Admin user creation request - username: {}, roles: {}",
                user.getUsername(), user.getUserRoles());

        // 1. Capture request context
        RequestContext requestContext = requestContextService.captureRequestContext(request);

        // 2. Convert to admin creation model
        AdminUserCreationRequest adminRequest = new AdminUserCreationRequest(
                user.getUsername(),
                user.getFirstname(),
                user.getLastname(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getUserRoles(),
                true,
                requestContext
        );

        // 3. Validate via admin policy service
        AdminValidationResult validation = adminPolicyService.validateUserCreation(adminRequest);
        if (!validation.isValid()) {
            String errorMessage = "User creation validation failed: " +
                    String.join(", ", validation.errors());
            log.warn(errorMessage);
            throw new AdminOperationException(errorMessage);
        }

        // 4. Get admin actor for permission checks
        User actor = userService.getAuthenticatedUser();

        // 5. Check role granting permissions
        for (UserRole role : user.getUserRoles()) {
            if (!userPermissionService.canGrantRole(actor, user, role)) {
                throw UserPermissionExceptionFactory.forInsufficientPermissions(
                        userPermissionService.getHighestRole(actor),
                        "grant role " + role);
            }
        }

        // 6. Delegate to user creation service
        UserCreationRequest userCreationRequest = UserCreationRequest.forAdminCreate(user, requestContext);
        UserCreationResult result = userCreationService.createUser(userCreationRequest);

        log.info("User successfully created by admin - ID: {}, username: {}",
                result.user().getId(), result.user().getUsername());

        return result.user();
    }

    @Override
    @Transactional
    public void activateUser(Long id, HttpServletRequest request) {
        log.debug("Admin activation request - targetId: {}", id);

        // 1. Capture request context
        RequestContext requestContext = requestContextService.captureRequestContext(request);

        // 2. Get actors
        User admin = userService.getAuthenticatedUser();
        User target = userService.getUserById(id);

        // 3. Determine activation type and delegate
        boolean isFirstActivation = !target.isEverActivated();

        if (isFirstActivation) {
            userService.adminActivateUser(target, admin);
        } else {
            userService.adminReactivateUser(target, admin);
        }

        // 4. Publish appropriate events
        boolean wasDeactivated = target.getDeactivatedAt() != null;
        AdminUserActivatedEvent event = wasDeactivated
                ? AdminUserActivatedEvent.of(target, admin, true, target.getDeactivatedAt(), requestContext)
                : AdminUserActivatedEvent.simple(target, admin, requestContext);

        eventPublisher.publishEvent(event);

        String actionType = isFirstActivation ? "activated" : "reactivated";
        log.info("User successfully {} by admin - ID: {}, {} by: {}",
                actionType, id, actionType, admin.getUsername());
    }

    @Override
    @Transactional
    public void deactivateUser(Long id, AdminDeactivationCategory deactivationCategory, String adminDeactivationDetails, HttpServletRequest request) {
        log.debug("Admin deactivation request - targetId: {}, category: {}",
                id, deactivationCategory);

        // 1. Capture request context
        RequestContext requestContext = requestContextService.captureRequestContext(request);

        // 2. Get actors
        User admin = userService.getAuthenticatedUser();
        User target = userService.getUserById(id);

        // 3. Validate deactivation details via admin policy
        AdminDeactivationRequest deactivationRequest = new AdminDeactivationRequest(
                id, deactivationCategory, adminDeactivationDetails);

        AdminValidationResult validationResult = adminPolicyService.validateDeactivationDetails(deactivationRequest);
        if (!validationResult.isValid()) {
            throw new AdminOperationException(
                    "Deactivation validation failed: " + String.join(", ", validationResult.errors()));
        }

        // 4. Delegate to user service
        userService.adminDeactivateUser(target, admin, deactivationCategory, adminDeactivationDetails);

        // 5. Publish deactivation events
        AdminUserDeactivatedEvent event = AdminUserDeactivatedEvent.simple(
                target, admin, deactivationCategory, adminDeactivationDetails, requestContext);
        eventPublisher.publishEvent(event);

        log.info("User successfully deactivated by admin - ID: {}, deactivated by: {}, category: {}",
                id, admin.getUsername(), deactivationCategory);
    }

    @Override
    
    @Transactional
    public void reactivateUser(Long id, HttpServletRequest request) {
        log.debug("Admin reactivation request - targetId: {}", id);

        // 1. Capture request context
        RequestContext requestContext = requestContextService.captureRequestContext(request);

        // 2. Get actors
        User admin = userService.getAuthenticatedUser();
        User target = userService.getUserById(id);

        // 3. Delegate to user service
        userService.adminReactivateUser(target, admin);

        // 4. Publish reactivation events
        AdminUserActivatedEvent event = AdminUserActivatedEvent.of(
                target, admin, true, target.getDeactivatedAt(), requestContext);
        eventPublisher.publishEvent(event);

        log.info("User successfully reactivated by admin - ID: {}, reactivated by: {}",
                id, admin.getUsername());
    }

    // ===============================
    // ROLE MANAGEMENT
    // ===============================

    @Override
    @Transactional
    public void grantUserRole(Long id, UserRole role) {
        log.debug("Admin role grant request - targetId: {}, role: {}", id, role);

        // 1. Get actors
        User actor = userService.getAuthenticatedUser();
        User target = userService.getUserById(id);

        // 2. Check permissions
        if (!userPermissionService.canGrantRole(actor, target, role)) {
            UserRole actorRole = userPermissionService.getHighestRole(actor);
            if (role == UserRole.SUPER_ADMIN || role == UserRole.ADMIN) {
                throw UserPermissionExceptionFactory.forInsufficientPermissions(
                        UserRole.SUPER_ADMIN, "grant role " + role);
            } else {
                throw UserPermissionExceptionFactory.forInsufficientPermissions(
                        UserRole.ADMIN, "grant role " + role);
            }
        }

        // 3. Delegate to user service
        userService.grantUserRole(id, role);

        log.info("Role {} granted to user {} by admin {}",
                role, target.getUsername(), actor.getUsername());
    }

    @Override
    @Transactional
    public void revokeUserRole(Long id, UserRole role) {
        log.debug("Admin role revoke request - targetId: {}, role: {}", id, role);

        // 1. Get actors
        User actor = userService.getAuthenticatedUser();
        User target = userService.getUserById(id);

        // 2. Check permissions
        if (!userPermissionService.canRevokeRole(actor, target, role)) {
            throw UserPermissionExceptionFactory.forInsufficientPermissions(
                    userPermissionService.getHighestRole(actor), "revoke role " + role);
        }

        // 3. Delegate to user service
        userService.revokeUserRole(id, role);

        log.info("Role {} revoked from user {} by admin {}",
                role, target.getUsername(), actor.getUsername());
    }

    // ===============================
    // OTHER ADMIN OPERATIONS
    // ===============================

    @Override
    @Transactional
    public void deleteUser(Long id) {
        // Delegate with permission check
        userService.deleteUser(id);
    }

    @Override
    @Transactional
    public void gdprUserDelete(User user) {
        // Delegate with permission check
        userService.gdprUserDelete(user);
    }

    @Override
    @Transactional
    public void triggerPasswordReset(Long id) {
        // 1. Check admin permissions
        userService.requireAdminPermissions();

        // 2. Get target user
        User target = getUserById(id);

        // 3. Create password reset token
        PasswordResetToken token = passwordResetTokenService.createPasswordResetToken(target);

        // 4. Send email
        mailerService.sendPasswordReset(token.getToken(), target);

        log.info("Password reset triggered for user {} by admin", target.getUsername());
    }

    // ===============================
    // QUERY OPERATIONS (Simple delegation)
    // ===============================

    @Override
    public Page<User> searchUsers(String query, Pageable pageable) {
        userService.requireAdminPermissions();
        return userService.searchUsers(query, pageable);
    }

    @Override
    public Page<User> searchUsersByCriteria(String username, String firstname, String lastname,
                                            String email, String phoneNumber, Pageable pageable) {
        userService.requireAdminPermissions();
        return userService.searchUsersByCriteria(username, firstname, lastname, email, phoneNumber, pageable);
    }

    @Override
    public User getUserById(Long id) {
        userService.requireAdminPermissions();
        return userService.getUserById(id);
    }

    @Override
    public List<Device> getUserDevices(Long id) {
        userService.requireAdminPermissions();
        User user = userService.getUserById(id);
        return deviceService.getUserDevice(user);
    }

    @Override
    public Long getTotalUsers() {
        userService.requireAdminPermissions();
        return userService.getTotalUsers();
    }
}