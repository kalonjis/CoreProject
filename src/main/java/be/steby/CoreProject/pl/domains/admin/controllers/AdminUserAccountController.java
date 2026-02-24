package be.steby.CoreProject.pl.domains.admin.controllers;

import be.steby.CoreProject.bll.domains.admin.services.useraccount.AdminUserAccountService;
import be.steby.CoreProject.pl.domains.account.models.responses.AccountOperationResponse;
import be.steby.CoreProject.pl.domains.admin.models.requests.AdminUserCreateRequest;
import be.steby.CoreProject.pl.domains.admin.models.requests.UserDeactivationForm;
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
     * @return 201 Created with operation response
     */
    @PostMapping("/create")
    public ResponseEntity<AccountOperationResponse> createUser(
            @Valid @RequestBody AdminUserCreateRequest request) {

        log.info("Admin user creation request - email: {}", request.email());

        adminUserAccountService.createUser(request.toBLL());

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
     * @param publicId User's publicId to activate
     * @return 200 OK with success message
     */
    @PatchMapping("/activate/{publicId}")
    public ResponseEntity<Map<String, String>> activateUser(
            @PathVariable String publicId
    ) {

        log.info("Admin activation request - userId: {}", publicId);

        adminUserAccountService.activateUser(publicId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "User activated successfully");
        response.put("userId", publicId.toString());

        log.info("User activated successfully - userId: {}", publicId);

        return ResponseEntity.ok(response);
    }

    // ===============================
    // USER DEACTIVATION
    // ===============================

    /**
     * Deactivates a user account as an administrator.
     * PATCH /api/admin/users/deactivate/{id}
     *
     * @param publicId User's publicId to deactivate
     * @param form Deactivation form with category and details
     * @return 200 OK with success message
     */
    @PatchMapping("/deactivate/{publicId}")
    public ResponseEntity<Map<String, String>> deactivateUser(
            @PathVariable String publicId,
            @Valid @RequestBody UserDeactivationForm form) {

        log.info("Admin deactivation request - userId: {}, category: {}",
                publicId, form.deactivationCategory());

        adminUserAccountService.deactivateUser(
                publicId,
                form.deactivationCategory(),
                form.adminDeactivationDetails()
        );

        Map<String, String> response = new HashMap<>();
        response.put("message", "User deactivated successfully");
        response.put("userId", publicId);
        response.put("category", form.deactivationCategory().name());

        log.info("User deactivated successfully - userId: {}", publicId);

        return ResponseEntity.ok(response);
    }
}