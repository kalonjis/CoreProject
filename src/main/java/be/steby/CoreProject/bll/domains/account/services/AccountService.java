package be.steby.CoreProject.bll.domains.account.services;

import be.steby.CoreProject.bll.domains.account.models.DeactivationRequest;
import be.steby.CoreProject.bll.domains.account.models.ReactivationRequest;
import be.steby.CoreProject.bll.domains.account.models.SelfSignupRequest;
import be.steby.CoreProject.dl.entities.User;

public interface AccountService {

    // =========================================================================
    // Signup
    // =========================================================================

    User signup(SelfSignupRequest request);

    User confirmNewUserAccount(String token);

    void resendActivation(String token);

    void resendActivationByIdentifier(String identifier);

    // =========================================================================
    // Deactivation (reversible)
    // =========================================================================

    void requestDeactivation(User user, DeactivationRequest deactivationRequest);

    User deactivateAccount(String token);

    // =========================================================================
    // Reactivation
    // =========================================================================

    void requestReactivation(ReactivationRequest reactivationRequest);

    User reactivateAccount(String token);

    // =========================================================================
    // GDPR Deletion (irreversible)
    // =========================================================================

    /**
     * Initiates a GDPR deletion request for the authenticated user.
     *
     * <p>Creates a single-use confirmation token and sends it by email.
     * No data is modified at this stage.
     *
     * @param user the authenticated user requesting deletion
     */
    void requestDeletion(User user);

    /**
     * Confirms and executes the GDPR deletion via the token received by email.
     *
     * <p>Anonymizes personal data, revokes all active sessions and tokens,
     * and sends a post-deletion acknowledgement email.
     *
     * @param token the raw token value from the confirmation email link
     */
    void confirmDeletion(String token);
}