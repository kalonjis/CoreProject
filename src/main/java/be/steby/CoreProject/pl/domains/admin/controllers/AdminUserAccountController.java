package be.steby.CoreProject.pl.domains.admin.controllers;

import be.steby.CoreProject.bll.domains.admin.services.useraccount.AdminUserAccountService;
import be.steby.CoreProject.pl.domains.account.models.responses.AccountOperationResponse;
import be.steby.CoreProject.pl.domains.admin.models.requests.AdminUserCreateRequest;
import be.steby.CoreProject.pl.domains.admin.models.requests.UserDeactivationForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for admin user account lifecycle operations.
 * Handles user creation, activation, and deactivation.
 *
 * All endpoints require ADMIN privileges.
 * Business logic is delegated to AdminUserAccountService.
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@RequestMapping("/api/admin/users")
@Slf4j
public class AdminUserAccountController {

    private final AdminUserAccountService adminUserAccountService;

    // ===============================
    // USER CREATION
    // ===============================

    /**
     * Creates a new user account as an administrator.
     * POST /api/admin/users
     *
     * @param request User creation request with all required data
     * @param httpRequest HTTP request for context capture
     * @return 201 Created with operation response
     */
    @PostMapping
    public ResponseEntity<AccountOperationResponse> createUser(
            @Valid @RequestBody AdminUserCreateRequest request,
            HttpServletRequest httpRequest) {

        log.info("Admin user creation request - email: {}", request.email());

        adminUserAccountService.createUser(request.toBLL(), httpRequest);

        log.info("User created successfully - email: {}", request.email());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(AccountOperationResponse.accountCreated());
    }

    // ===============================
    // USER ACTIVATION
    // ===============================

    /**
     * Activates or reactivates a user account.
     * Automatically determines if it's first activation or reactivation.
     * PATCH /api/admin/users/activate/{id}
     *
     * @param id User ID to activate
     * @param request HTTP request for context capture
     * @return 200 OK with success message
     */
    @PatchMapping("/activate/{id}")
    public ResponseEntity<Map<String, String>> activateUser(
            @PathVariable Long id,
            HttpServletRequest request) {

        log.info("Admin activation request - userId: {}", id);

        adminUserAccountService.activateUser(id, request);

        Map<String, String> response = new HashMap<>();
        response.put("message", "User activated successfully");
        response.put("userId", id.toString());

        log.info("User activated successfully - userId: {}", id);

        return ResponseEntity.ok(response);
    }

    // ===============================
    // USER DEACTIVATION
    // ===============================

    /**
     * Deactivates a user account as an administrator.
     * PATCH /api/admin/users/deactivate/{id}
     *
     * @param id User ID to deactivate
     * @param form Deactivation form with category and details
     * @param request HTTP request for context capture
     * @return 200 OK with success message
     */
    @PatchMapping("/deactivate/{id}")
    public ResponseEntity<Map<String, String>> deactivateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserDeactivationForm form,
            HttpServletRequest request) {

        log.info("Admin deactivation request - userId: {}, category: {}",
                id, form.deactivationCategory());

        adminUserAccountService.deactivateUser(
                id,
                form.deactivationCategory(),
                form.adminDeactivationDetails(),
                request
        );

        Map<String, String> response = new HashMap<>();
        response.put("message", "User deactivated successfully");
        response.put("userId", id.toString());
        response.put("category", form.deactivationCategory().name());

        log.info("User deactivated successfully - userId: {}", id);

        return ResponseEntity.ok(response);
    }
}