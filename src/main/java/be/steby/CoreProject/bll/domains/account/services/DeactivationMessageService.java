package be.steby.CoreProject.bll.domains.account.services;

import be.steby.CoreProject.dl.enums.DeactivationReason;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service responsible for managing deactivation-related messages and business rules.
 *
 * This service encapsulates:
 * - Business rules for account reactivation eligibility
 * - Personalized messaging based on deactivation reasons
 * - Email content generation for deactivation workflows
 *
 * @author Generated
 * @since 1.0
 */
@Service
@Slf4j
public class DeactivationMessageService {

    // region Business Rules

    /**
     * Determines if the deactivation reason allows for account reactivation.
     *
     * This method implements core business rules for account reactivation:
     * - GDPR_REQUEST: Cannot be reactivated (data permanently deleted)
     * - ADMIN_DECISION: Cannot be automatically reactivated (requires manual review)
     * - All other reasons: Allow automatic reactivation
     *
     * @param reason The deactivation reason to evaluate
     * @return true if reactivation is allowed, false otherwise
     */
    public boolean isReactivationAllowed(DeactivationReason reason) {
        if (reason == null) {
            return true; // Default allows reactivation
        }

        return switch (reason) {
            case GDPR_REQUEST -> false; // GDPR deletion is permanent
            default -> true; // All other reasons allow reactivation
        };
    }

    // endregion

    // region Confirmation Messages

    /**
     * Returns an appropriate confirmation message based on the deactivation reason.
     * Used in post-deactivation confirmation emails.
     *
     * @param reason The reason for deactivation
     * @param reasonDetails Additional details provided by the user (can be null)
     * @return A personalized message for the confirmation email
     */
    public String getConfirmationMessage(DeactivationReason reason, String reasonDetails) {
        if (reason == null) {
            return getDefaultConfirmationMessage();
        }

        return switch (reason) {
            case TAKING_A_BREAK ->
                    "We understand that sometimes we all need to take a step back. Your account has been successfully deactivated. " +
                            "Take all the time you need, and remember that you can always reactivate your account when you're ready to return. " +
                            "We'll be here waiting for you!";

            case TOO_MUCH_TIME ->
                    "We appreciate your awareness about digital wellness and taking control of your time. " +
                            "Your account has been deactivated as requested. We respect your decision to prioritize your time, " +
                            "and your account will be ready for you whenever you decide to return.";

            case PRIVACY_CONCERNS ->
                    "We take privacy seriously and understand your concerns. Your account has been successfully deactivated. " +
                            "If you have specific privacy questions or feedback that might help us improve, " +
                            "please don't hesitate to contact our support team. Your trust means everything to us.";

            case ACCOUNT_CLEANUP ->
                    "Your account has been successfully deactivated as part of your account cleanup process. " +
                            "We understand the importance of managing your digital footprint. " +
                            "If you change your mind, you can always reactivate your account in the future.";

            case SWITCHING_ACCOUNTS ->
                    "Your account has been deactivated successfully. We understand you're moving to a different account. " +
                            "If you need any assistance with your transition or have questions about your data, " +
                            "our support team is here to help.";

            case WORK_REQUIREMENTS ->
                    "Your account has been deactivated due to work requirements. We understand that professional " +
                            "obligations sometimes require these changes. Your account information is safely preserved " +
                            "and can be reactivated when your situation allows.";

            case NOT_USEFUL ->
                    "Your account has been successfully deactivated. We're sorry that our application didn't meet your needs. " +
                            "Your feedback is valuable to us, and we're constantly working to improve our services. " +
                            "You're always welcome to give us another try in the future.";

            case GDPR_REQUEST ->
                    "Your account has been deactivated and your personal data has been processed according to GDPR regulations. " +
                            "We have honored your right to be forgotten and have safely removed or anonymized your personal information " +
                            "as required by European data protection law. If you have any questions about this process, " +
                            "please contact our Data Protection Officer.";

            case OTHER -> {
                String baseMessage = "Your account has been successfully deactivated for the reason you specified.";
                if (reasonDetails != null && !reasonDetails.trim().isEmpty()) {
                    yield baseMessage + " We've noted your feedback: \"" + reasonDetails.trim() + "\". " +
                            "Thank you for taking the time to provide additional context.";
                }
                yield baseMessage + " If you need any assistance or have questions, our support team is available to help.";
            }
        };
    }

    // endregion

    // region Request Messages

    /**
     * Returns a personalized message for the deactivation request email based on the reason.
     * Used in the initial deactivation request confirmation email.
     *
     * @param reason The reason for deactivation
     * @param reasonDetails Additional details provided by the user (can be null)
     * @return A personalized message for the deactivation request email
     */
    public String getDeactivationRequestMessage(DeactivationReason reason, String reasonDetails) {
        if (reason == null) {
            return "We have received a request to deactivate your MyFavApp account.";
        }

        return switch (reason) {
            case TAKING_A_BREAK ->
                    "We have received your request to deactivate your account for taking a break. " +
                            "We completely understand the need to step back sometimes.";

            case TOO_MUCH_TIME ->
                    "We have received your request to deactivate your account due to time management concerns. " +
                            "We respect your decision to prioritize your digital wellness.";

            case PRIVACY_CONCERNS ->
                    "We have received your request to deactivate your account due to privacy concerns. " +
                            "We take your privacy seriously and want to address any concerns you may have.";

            case ACCOUNT_CLEANUP ->
                    "We have received your request to deactivate your account as part of your account cleanup process. " +
                            "We understand the importance of managing your digital presence.";

            case SWITCHING_ACCOUNTS ->
                    "We have received your request to deactivate your account because you're switching to another account. " +
                            "We're here to help make this transition as smooth as possible.";

            case WORK_REQUIREMENTS ->
                    "We have received your request to deactivate your account due to work requirements. " +
                            "We understand that professional obligations sometimes necessitate these changes.";

            case NOT_USEFUL ->
                    "We have received your request to deactivate your account as our application no longer meets your needs. " +
                            "We appreciate your feedback and are sorry to see you go.";

            case GDPR_REQUEST ->
                    "We have received your request to delete your account and personal data in accordance with GDPR regulations. " +
                            "We will process this request according to European data protection law.";

            case OTHER -> {
                String baseMessage = "We have received your request to deactivate your account.";
                if (reasonDetails != null && !reasonDetails.trim().isEmpty()) {
                    yield baseMessage + " You mentioned: \"" + reasonDetails.trim() + "\"";
                }
                yield baseMessage;
            }
        };
    }

    // endregion

    // region Subject Lines

    /**
     * Returns a subject line for the deactivation request email based on the reason.
     *
     * @param reason The deactivation reason
     * @return Appropriate subject line for the request email
     */
    public String getDeactivationRequestSubjectLine(DeactivationReason reason) {
        if (reason == null) {
            return "Confirm Your Account Deactivation – MyFavApp";
        }

        return switch (reason) {
            case TAKING_A_BREAK -> "Taking a Break – Confirm Deactivation – MyFavApp";
            case TOO_MUCH_TIME -> "Digital Wellness – Confirm Deactivation – MyFavApp";
            case PRIVACY_CONCERNS -> "Privacy Concerns – Confirm Deactivation – MyFavApp";
            case ACCOUNT_CLEANUP -> "Account Cleanup – Confirm Deactivation – MyFavApp";
            case SWITCHING_ACCOUNTS -> "Account Switch – Confirm Deactivation – MyFavApp";
            case WORK_REQUIREMENTS -> "Work Requirements – Confirm Deactivation – MyFavApp";
            case NOT_USEFUL -> "Confirm Your Account Deactivation – MyFavApp";
            case GDPR_REQUEST -> "GDPR Request – Confirm Account Deletion – MyFavApp";
            case OTHER -> "Confirm Your Account Deactivation – MyFavApp";
        };
    }

    /**
     * Returns a subject line for the deactivation confirmation email based on the reason.
     *
     * @param reason The deactivation reason
     * @return Appropriate subject line for the confirmation email
     */
    public String getSubjectLine(DeactivationReason reason) {
        if (reason == null) {
            return "Account Deactivation Confirmed – MyFavApp";
        }

        return switch (reason) {
            case TAKING_A_BREAK -> "Taking a Break – Account Deactivated – MyFavApp";
            case TOO_MUCH_TIME -> "Digital Wellness – Account Deactivated – MyFavApp";
            case PRIVACY_CONCERNS -> "Privacy First – Account Deactivated – MyFavApp";
            case ACCOUNT_CLEANUP -> "Account Cleanup – Deactivation Confirmed – MyFavApp";
            case SWITCHING_ACCOUNTS -> "Account Switch – Deactivation Confirmed – MyFavApp";
            case WORK_REQUIREMENTS -> "Work Requirements – Account Deactivated – MyFavApp";
            case NOT_USEFUL -> "Account Deactivated – MyFavApp";
            case GDPR_REQUEST -> "GDPR Compliance – Account Deactivated – MyFavApp";
            case OTHER -> "Account Deactivation Confirmed – MyFavApp";
        };
    }

    // endregion

    // region Helper Methods

    /**
     * Returns a default confirmation message when no specific reason is provided.
     *
     * @return Default confirmation message
     */
    private String getDefaultConfirmationMessage() {
        return "Your account has been successfully deactivated. We're sorry to see you go, but we understand " +
                "that everyone's needs are different. Your account can be reactivated at any time if you decide to return. " +
                "Thank you for being part of our community.";
    }

    /**
     * Logs the evaluation of reactivation eligibility for audit purposes.
     *
     * @param reason The deactivation reason being evaluated
     * @param allowed Whether reactivation is allowed
     */
    public void logReactivationEligibility(DeactivationReason reason, boolean allowed) {
        log.debug("Reactivation eligibility check: reason={}, allowed={}", reason, allowed);

        if (!allowed) {
            log.info("Reactivation denied for reason: {} (requires manual intervention)", reason);
        }
    }

    // endregion
}