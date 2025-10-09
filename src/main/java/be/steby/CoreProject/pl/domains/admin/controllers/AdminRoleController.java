package be.steby.CoreProject.pl.domains.admin.controllers;

import be.steby.CoreProject.bll.domains.admin.services.role.AdminRoleService;
import be.steby.CoreProject.pl.domains.admin.models.requests.UserRoleForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for admin user role management operations.
 * Handles role granting and revocation.
 *
 * All endpoints require ADMIN privileges.
 * Permission validation is performed based on role hierarchy:
 * - SUPER_ADMIN can grant/revoke any role
 * - ADMIN can grant/revoke MODERATOR and USER roles only
 *
 * Business logic is delegated to AdminRoleService.
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@RequestMapping("/api/admin/users")
@Slf4j
public class AdminRoleController {

    private final AdminRoleService adminRoleService;

    // ===============================
    // ROLE GRANTING
    // ===============================

    /**
     * Grants a role to a user.
     * PATCH /api/admin/users/grant-role/{id}
     *
     * Permission rules:
     * - SUPER_ADMIN can grant any role to anyone
     * - ADMIN can grant MODERATOR and USER roles to anyone
     * - ADMIN cannot grant ADMIN or SUPER_ADMIN roles
     *
     * @param id User ID receiving the role
     * @param form Role form containing the role to grant
     * @param request HTTP request for context capture
     * @return 200 OK with success message
     */
    @PatchMapping("/grant-role/{id}")
    public ResponseEntity<Map<String, String>> grantUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UserRoleForm form,
            HttpServletRequest request) {

        log.info("Admin grant role request - userId: {}, role: {}", id, form.userRole());

        adminRoleService.grantRole(id, form.userRole());

        Map<String, String> response = new HashMap<>();
        response.put("message", "Role granted successfully");
        response.put("userId", id.toString());
        response.put("role", form.userRole().name());

        log.info("Role granted successfully - userId: {}, role: {}", id, form.userRole());

        return ResponseEntity.ok(response);
    }

    // ===============================
    // ROLE REVOCATION
    // ===============================

    /**
     * Revokes a role from a user.
     * PATCH /api/admin/users/revoke-role/{id}
     *
     * Permission rules:
     * - SUPER_ADMIN can revoke any role from anyone
     * - ADMIN can revoke MODERATOR and USER roles from lower hierarchy users only
     * - ADMIN cannot revoke roles from other ADMINs or SUPER_ADMINs
     *
     * @param id User ID losing the role
     * @param form Role form containing the role to revoke
     * @param request HTTP request for context capture
     * @return 200 OK with success message
     */
    @PatchMapping("/revoke-role/{id}")
    public ResponseEntity<Map<String, String>> revokeUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UserRoleForm form,
            HttpServletRequest request) {

        log.info("Admin revoke role request - userId: {}, role: {}", id, form.userRole());

        adminRoleService.revokeRole(id, form.userRole());

        Map<String, String> response = new HashMap<>();
        response.put("message", "Role revoked successfully");
        response.put("userId", id.toString());
        response.put("role", form.userRole().name());

        log.info("Role revoked successfully - userId: {}, role: {}", id, form.userRole());

        return ResponseEntity.ok(response);
    }
}