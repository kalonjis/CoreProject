package be.steby.CoreProject.pl.domains.admin.controllers;

import be.steby.CoreProject.bll.domains.admin.services.useraccount.AdminUserAccountService;
import be.steby.CoreProject.dl.enums.admin.deactivation.AdminDeactivationCategory;
import be.steby.CoreProject.pl.domains.admin.models.requests.AdminUserCreateRequest;
import be.steby.CoreProject.pl.domains.admin.models.requests.UserDeactivationForm;
import be.steby.CoreProject.pl.domains.admin.models.responses.AdminAccountOperationResponse;
import be.steby.CoreProject.pl.domains.admin.models.responses.DeactivationCategoryDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * REST controller for admin-initiated user account lifecycle operations.
 *
 * <h3>Responsibilities</h3>
 * <p>This controller handles HTTP concerns only:
 * <ul>
 *   <li>Request deserialization and validation</li>
 *   <li>HTTP status mapping</li>
 *   <li>Response serialization</li>
 * </ul>
 * All business logic is delegated to {@link AdminUserAccountService}.
 *
 * <h3>Security</h3>
 * <p>All endpoints require at minimum {@code ADMIN} authority.
 * Endpoints marked with {@code @PreAuthorize("hasAuthority('SUPER_ADMIN')")} require
 * elevated privileges enforced both at the HTTP layer and at the service layer.
 *
 * <h3>Base path</h3>
 * <pre>/api/admin/users</pre>
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAuthority('ADMIN')")
@RequestMapping("/api/admin/users")
public class AdminUserAccountController {

    private final AdminUserAccountService adminUserAccountService;

    // =========================================================================
    // USER CREATION
    // =========================================================================

    /**
     * Creates a new user account as an administrator.
     *
     * <p>The created account is immediately enabled with a temporary password
     * sent to the user by email. The user must change it on first login.
     *
     * <pre>POST /api/admin/users/create</pre>
     *
     * @param request creation payload (firstname, lastname, email, phone, roles)
     * @return 201 Created
     */
    @PostMapping("/create")
    public ResponseEntity<AdminAccountOperationResponse> createUser(
            @Valid @RequestBody AdminUserCreateRequest request) {

        log.info("Admin user creation request — email: {}", request.email());

        adminUserAccountService.createUser(request.toBLL());

        log.info("User created successfully — email: {}", request.email());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(AdminAccountOperationResponse.userCreated());
    }

    // =========================================================================
    // USER ACTIVATION  (first-time — everActivated == false)
    // =========================================================================

    /**
     * Activates a user account that was created by an admin and has never logged in.
     *
     * <p>Use this endpoint for accounts with {@code everActivated == false}.
     * For previously activated accounts, use {@code PATCH /reactivate/{publicId}}.
     *
     * <pre>PATCH /api/admin/users/activate/{publicId}</pre>
     *
     * @param publicId public UUID of the target user
     * @return 200 OK
     */
    @PatchMapping("/activate/{publicId}")
    public ResponseEntity<AdminAccountOperationResponse> activateUser(
            @PathVariable String publicId) {

        log.info("Admin activation request — publicId: {}", publicId);

        adminUserAccountService.activateUser(publicId);

        log.info("User activated successfully — publicId: {}", publicId);
        return ResponseEntity.ok(AdminAccountOperationResponse.userActivated());
    }

    // =========================================================================
    // USER REACTIVATION  (everActivated == true && enabled == false)
    // =========================================================================

    /**
     * Reactivates a previously deactivated user account.
     *
     * <p>Use this endpoint for accounts with {@code everActivated == true && enabled == false}.
     * For first-time activation, use {@code PATCH /activate/{publicId}}.
     *
     * <pre>PATCH /api/admin/users/reactivate/{publicId}</pre>
     *
     * @param publicId public UUID of the target user
     * @return 200 OK
     */
    @PatchMapping("/reactivate/{publicId}")
    public ResponseEntity<AdminAccountOperationResponse> reactivateUser(
            @PathVariable String publicId) {

        log.info("Admin reactivation request — publicId: {}", publicId);

        adminUserAccountService.reactivateUser(publicId);

        log.info("User reactivated successfully — publicId: {}", publicId);
        return ResponseEntity.ok(AdminAccountOperationResponse.userReactivated());
    }

    // =========================================================================
    // USER DEACTIVATION
    // =========================================================================


    /**
     * Returns all available deactivation categories.
     * Used by the admin UI to populate the category picker in the deactivation modal.
     *
     * GET /api/admin/users/deactivation-categories
     */
    @GetMapping("/deactivation-categories")
    public ResponseEntity<List<DeactivationCategoryDTO>> getDeactivationCategories() {
        List<DeactivationCategoryDTO> categories = Arrays.stream(AdminDeactivationCategory.values())
                .map(DeactivationCategoryDTO::fromEnum)
                .toList();

        return ResponseEntity.ok(categories);
    }

    /**
     * Deactivates a user account by administrative decision.
     *
     * <p>Requires a deactivation category and a mandatory justification.
     * The deactivation is tracked in dedicated audit fields, separate from
     * self-deactivation data.
     *
     * <pre>PATCH /api/admin/users/deactivate/{publicId}</pre>
     *
     * @param publicId public UUID of the target user
     * @param form     deactivation payload (category + details)
     * @return 200 OK
     */
    @PatchMapping("/deactivate/{publicId}")
    public ResponseEntity<AdminAccountOperationResponse> deactivateUser(
            @PathVariable String publicId,
            @Valid @RequestBody UserDeactivationForm form) {

        log.info("Admin deactivation request — publicId: {}, category: {}",
                publicId, form.deactivationCategory());

        adminUserAccountService.deactivateUser(publicId, form.deactivationCategory(),
                form.adminDeactivationDetails());

        log.info("User deactivated successfully — publicId: {}", publicId);
        return ResponseEntity.ok(AdminAccountOperationResponse.userDeactivated());
    }

    // =========================================================================
    // USER DELETION (SUPER_ADMIN only)
    // =========================================================================

    /**
     * Permanently deletes a user account and all associated data.
     *
     * <p><strong>Irreversible.</strong> Use with extreme caution.
     * Requires {@code SUPER_ADMIN} authority.
     *
     * <pre>DELETE /api/admin/users/delete/{publicId}</pre>
     *
     * @param publicId public UUID of the target user
     * @return 200 OK
     */
    @DeleteMapping("/delete/{publicId}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<AdminAccountOperationResponse> deleteUser(
            @PathVariable String publicId) {

        log.info("Admin hard-delete request — publicId: {}", publicId);

        adminUserAccountService.deleteUser(publicId);

        log.info("User permanently deleted — publicId: {}", publicId);
        return ResponseEntity.ok(AdminAccountOperationResponse.userDeleted());
    }

    /**
     * Anonymizes a user's personal data under GDPR right-to-erasure.
     *
     * <p>Preserves the user record for referential integrity while permanently
     * removing all personally identifiable information.
     * Requires {@code SUPER_ADMIN} authority.
     *
     * <pre>DELETE /api/admin/users/gdpr/{publicId}</pre>
     *
     * @param publicId public UUID of the target user
     * @return 200 OK
     */
    @DeleteMapping("/gdpr/{publicId}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    public ResponseEntity<AdminAccountOperationResponse> gdprDeleteUser(
            @PathVariable String publicId) {

        log.info("Admin GDPR deletion request — publicId: {}", publicId);

        adminUserAccountService.gdprDeleteUser(publicId);

        log.info("User GDPR-anonymized successfully — publicId: {}", publicId);
        return ResponseEntity.ok(AdminAccountOperationResponse.userGdprDeleted());
    }
}