package be.steby.CoreProject.bll.domains.account.services;

import be.steby.CoreProject.bll.domains.account.models.DeactivationRequest;
import be.steby.CoreProject.bll.domains.account.models.DeactivationValidationResult;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.DeactivationReason;


/**
 * Service interface for account deactivation policy validation.
 * Defines business rules and validation logic for account deactivation requests.
 */
public interface DeactivationPolicyService {

    /**
     * Validates a deactivation request against business rules.
     *
     * @param request The deactivation request to validate
     * @param user The user requesting deactivation
     * @return DeactivationValidationResult containing validation status and any error messages
     */
    DeactivationValidationResult validateDeactivationRequest(DeactivationRequest request, User user);


    /**
     * Validates only the deactivation reason and details without user context.
     * Useful for form-level validation.
     *
     * @param request The deactivation request to validate
     * @return DeactivationValidationResult containing validation status and any error messages
     */
    DeactivationValidationResult validateDeactivationReason(DeactivationRequest request);
}
