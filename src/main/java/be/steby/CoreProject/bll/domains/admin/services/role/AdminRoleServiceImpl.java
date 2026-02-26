package be.steby.CoreProject.bll.domains.admin.services.role;

import be.steby.CoreProject.bll.domains.admin.events.role.AdminRoleGrantedEvent;
import be.steby.CoreProject.bll.domains.admin.events.role.AdminRoleRevokedEvent;
import be.steby.CoreProject.bll.domains.admin.exceptions.AdminOperationException;
import be.steby.CoreProject.bll.domains.admin.exceptions.InvalidAdminArgumentException;
import be.steby.CoreProject.bll.domains.admin.services.permissions.AdminPermissionValidator;
import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for admin operations on user roles.
 * Handles role assignment and revocation with proper permission validation.
 *
 * Responsibilities:
 * - Orchestrates role management operations (business logic)
 * - Validates permissions (who can grant/revoke what)
 * - Publishes domain events (for notifications, audit, etc.)
 * - Delegates technical operations to specialized services
 *
 * Delegation strategy:
 * - UserService → Core user persistence and role state management
 * - UserPermissionService → Permission validation
 * - ApplicationEventPublisher → Event publishing for audit/notifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminRoleServiceImpl implements AdminRoleService {

    private final UserService userService;
    private final AdminPermissionValidator adminPermissionValidator;
    private final ApplicationEventPublisher eventPublisher;
    private final DeviceService deviceService;

    // ===============================
    // ROLE GRANTING
    // ===============================

    @Override
    @Transactional
    public void grantRole(String publicId, UserRole role) {


        // 1. Get actors
        User actor = userService.getAuthenticatedUser();
        User target = userService.getUserByPublicId(publicId);

        log.debug("Admin role grant request - targetId: {}, role: {}", target.getId(), role);

        // check argument coherence
        if (target.hasRole(role)){
            throw new InvalidAdminArgumentException("the user with id " + target.getId() + "has already the role of :" + role);
        }

        // role == SUPER_ADMIN -> actor must be SUPER_ADMIN
        if (role == UserRole.SUPER_ADMIN ){
            adminPermissionValidator.validateSuperAdminAction(actor, target, false, "grant-Super_Admin-role");
        }

        // role == ADMIN -> actor must be SUPER_ADMIN (may change so keep it separate
        if (role == UserRole.ADMIN){
            adminPermissionValidator.validateSuperAdminAction(actor, target, false, "grant-Admin-role");
        }

        if (role != UserRole.SUPER_ADMIN && role != UserRole.ADMIN){
            adminPermissionValidator.validateStrictHierarchy(actor, target, true, "grant-role : " + role);
        }

        target.getUserRoles().add(role);
        userService.saveUser(target);

        // 4. Publish role granted event
        Device device = deviceService.detectCurrentDevice();
        eventPublisher.publishEvent(new AdminRoleGrantedEvent(target, actor, device, role) );

        log.info("Role {} successfully granted to user {} by admin {}",
                role, target.getId(), actor.getId());
    }

    // ===============================
    // ROLE REVOCATION
    // ===============================

    @Override
    @Transactional
    public void revokeRole(String publicId, UserRole role) {

        // 1. Get actors
        User actor = userService.getAuthenticatedUser();
        User target = userService.getUserByPublicId(publicId);

        log.debug("Admin role revoke request - targetId: {}, role: {}", target.getId(), role);

        // check argument coherence
        if (!target.hasRole(role)){
            throw new InvalidAdminArgumentException("the user with id " + target.getId() + "has not the role of :" + role);
        }


        // role == SUPER_ADMIN -> not possible
        if (role == UserRole.SUPER_ADMIN ){
            throw new AdminOperationException("No possibilities to revoke Super_Admin role. Please contact CEO" );
        }

        // role == ADMIN -> actor must be SUPER_ADMIN
        if (role == UserRole.ADMIN){
            adminPermissionValidator.validateSuperAdminAction(actor, target, false, "grant-Admin-role");
        }

        if (role != UserRole.SUPER_ADMIN && role != UserRole.ADMIN){
            adminPermissionValidator.validateStrictHierarchy(actor, target, false, "revoke-role : " + role);
        }

        target.getUserRoles().remove(role);
        userService.saveUser(target);

        // 4. Publish role revoked event
        Device device = deviceService.detectCurrentDevice();
        eventPublisher.publishEvent(new AdminRoleRevokedEvent(target, actor, device, role ));

        log.info("Role {} successfully revoked from user {} by admin {}",
                role, target.getId(), actor.getId());
    }
}