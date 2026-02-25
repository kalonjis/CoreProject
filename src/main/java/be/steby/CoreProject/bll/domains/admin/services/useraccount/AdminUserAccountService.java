package be.steby.CoreProject.bll.domains.admin.services.useraccount;

import be.steby.CoreProject.bll.common.exceptions.AttributeUnchangedException;
import be.steby.CoreProject.bll.common.exceptions.UserPermissionException;
import be.steby.CoreProject.bll.domains.admin.exceptions.AdminOperationException;
import be.steby.CoreProject.bll.domains.admin.models.account.AdminDeactivationRequest;
import be.steby.CoreProject.bll.domains.admin.models.account.AdminUserCreationRequest;
import be.steby.CoreProject.bll.domains.user.exceptions.UserNotFoundException;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.admin.deactivation.AdminDeactivationCategory;

/**
 * Service contract for admin-initiated user account lifecycle operations.
 *
 * <h3>Separation of Concerns</h3>
 * <p>This service is responsible for <strong>orchestration only</strong>:
 * <ol>
 *   <li>Resolve actors (admin + target) from the security context</li>
 *   <li>Validate permissions via {@code AdminPermissionValidator}</li>
 *   <li>Validate business rules via {@code AdminActionPolicyService}</li>
 *   <li>Delegate state mutations to {@code UserService}</li>
 *   <li>Publish domain events for audit and notifications</li>
 * </ol>
 *
 * <p><strong>This service never mutates User fields directly.</strong>
 * All field-level changes are owned by {@code UserService}.
 *
 * <h3>Related services</h3>
 * <ul>
 *   <li>{@code AdminRoleService} — role grant / revoke operations</li>
 *   <li>{@code AdminSearchService} — read-only user queries</li>
 *   <li>{@code AdminPasswordService} — password reset flows</li>
 * </ul>
 */
public interface AdminUserAccountService {

    // =========================================================================
    // USER CREATION
    // =========================================================================

    /**
     * Creates a new user account as an administrator.
     *
     * <p>The created account will have:
     * <ul>
     *   <li>Email verified (admin vouches for the identity)</li>
     *   <li>Account immediately enabled</li>
     *   <li>A temporary password the user must change on first login</li>
     *   <li>A complete profile (firstname / lastname provided by admin)</li>
     * </ul>
     *
     * @param request creation payload with email, name, phone, and roles
     * @return the persisted user entity
     * @throws AdminOperationException  if request validation fails
     * @throws UserPermissionException  if the actor lacks permission to assign the requested roles
     */
    User createUser(AdminUserCreationRequest request);

    // =========================================================================
    // USER ACTIVATION  (first-time only — everActivated == false)
    // =========================================================================

    /**
     * Activates a user account that was created by an admin and has never logged in.
     *
     * <p>Use this when {@code everActivated == false}.
     * For accounts that were previously active and then deactivated, use
     * {@link #reactivateUser(String)} instead.
     *
     * @param publicId public UUID of the user to activate
     * @throws UserNotFoundException        if no user exists with that publicId
     * @throws UserPermissionException      if the actor lacks permission
     * @throws AttributeUnchangedException  if the user is already active
     * @throws IllegalStateException        if the user was already activated before
     */
    void activateUser(String publicId);

    // =========================================================================
    // USER REACTIVATION  (everActivated == true && enabled == false)
    // =========================================================================

    /**
     * Reactivates a previously deactivated user account.
     *
     * <p>Use this when {@code everActivated == true && enabled == false}.
     * For first-time activation of admin-created accounts, use
     * {@link #activateUser(String)} instead.
     *
     * <p>Reactivation may be blocked depending on the deactivation category
     * and the {@code ReactivationPolicy} attached to it.
     *
     * @param publicId public UUID of the user to reactivate
     * @throws UserNotFoundException        if no user exists with that publicId
     * @throws UserPermissionException      if the actor lacks permission or policy blocks reactivation
     * @throws AttributeUnchangedException  if the user is already active
     * @throws IllegalStateException        if the user was never activated before
     */
    void reactivateUser(String publicId);

    // =========================================================================
    // USER DEACTIVATION
    // =========================================================================

    /**
     * Deactivates a user account by administrative decision.
     *
     * <p>Admin deactivation is tracked in dedicated fields
     * ({@code adminDeactivation*}) and is completely separate from
     * self-deactivation data, preserving audit integrity.
     *
     * @param publicId   public UUID of the user to deactivate
     * @param category   administrative deactivation category
     * @param details    mandatory free-text justification
     * @throws UserNotFoundException        if no user exists with that publicId
     * @throws UserPermissionException      if the actor lacks permission
     * @throws AttributeUnchangedException  if the user is already deactivated
     * @throws AdminOperationException      if deactivation validation fails
     */
    void deactivateUser(String publicId, AdminDeactivationCategory category, String details);

    /**
     * Deactivates a user account using a structured request object.
     * Convenience overload that delegates to {@link #deactivateUser(String, AdminDeactivationCategory, String)}.
     *
     * @param publicId public UUID of the user to deactivate
     * @param request  deactivation payload with category and details
     */
    void deactivateUser(String publicId, AdminDeactivationRequest request);

    // =========================================================================
    // USER DELETION
    // =========================================================================

    /**
     * Permanently deletes a user account and all associated data (SUPER_ADMIN only).
     *
     * <p>This is an <strong>irreversible</strong> hard delete. Use with extreme caution.
     * For most cases, deactivation or GDPR anonymization should be preferred.
     *
     * @param publicId public UUID of the user to delete
     * @throws UserNotFoundException    if no user exists with that publicId
     * @throws UserPermissionException  if the actor is not a SUPER_ADMIN
     */
    void deleteUser(String publicId);

    /**
     * Anonymizes a user's personal data under the GDPR right-to-erasure flow (SUPER_ADMIN only).
     *
     * <p>Unlike {@link #deleteUser(String)}, this preserves the user record for
     * referential integrity while removing all personally identifiable information.
     *
     * @param publicId public UUID of the user to anonymize
     * @throws UserNotFoundException    if no user exists with that publicId
     * @throws UserPermissionException  if the actor is not a SUPER_ADMIN
     */
    void gdprDeleteUser(String publicId);
}