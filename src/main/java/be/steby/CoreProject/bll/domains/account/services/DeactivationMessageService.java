package be.steby.CoreProject.bll.domains.account.services;

import be.steby.CoreProject.dl.enums.DeactivationReason;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service responsible for managing deactivation-related messages.
 *
 * This service focuses purely on message generation for deactivation workflows:
 * - Personalized messaging based on deactivation reasons
 * - Email content generation for deactivation workflows
 * - Subject line generation
 *
 * Note: Business rules for reactivation eligibility are now handled
 * directly by the DeactivationReason and AdminDeactivationCategory enums.
 */
@Service
@Slf4j
public class DeactivationMessageService {

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
                    "We take privacy very seriously and understand your concerns. Your account has been deactivated as requested. " +
                            "We're constantly working to improve our privacy measures. If you decide to return in the future, " +
                            "you can reactivate your account at any time.";

            case ACCOUNT_CLEANUP ->
                    "Your account cleanup request has been processed successfully. Your account is now deactivated. " +
                            "If you change your mind, you can easily reactivate your account whenever you wish.";

            case SWITCHING_ACCOUNTS ->
                    "Your account has been deactivated as part of your account switching process. " +
                            "If you need to return to this account in the future, reactivation is always available.";

            case WORK_REQUIREMENTS ->
                    "We understand that work requirements sometimes necessitate account changes. " +
                            "Your account has been deactivated as requested. Should your work situation change, " +
                            "you can reactivate your account at any time.";

            case NOT_USEFUL ->
                    "We're sorry to hear that our service wasn't meeting your needs. Your account has been deactivated. " +
                            "We're always working to improve our platform. If you'd like to give us another try in the future, " +
                            "your account can be easily reactivated.";

            case GDPR_REQUEST ->
                    "Your GDPR deletion request has been processed. Your account and associated data have been permanently removed " +
                            "in compliance with data protection regulations. This action cannot be reversed.";

            case OTHER ->
                    "Your account has been deactivated as requested. We understand that everyone's needs are different. " +
                            "If you decide to return, you can reactivate your account at any time.";
        };
    }

    /**
     * Returns an appropriate deactivation request message.
     * Used in emails sent when user requests account deactivation.
     *
     * @param reason The reason for deactivation
     * @param reasonDetails Additional details provided by the user (can be null)
     * @return A personalized message for the deactivation request email
     */
    public String getDeactivationRequestMessage(DeactivationReason reason, String reasonDetails) {
        if (reason == null) {
            return "Please confirm your account deactivation request.";
        }

        String baseMessage = switch (reason) {
            case TAKING_A_BREAK -> "You've requested to deactivate your account to take a break.";
            case TOO_MUCH_TIME -> "You've requested to deactivate your account due to time management concerns.";
            case PRIVACY_CONCERNS -> "You've requested to deactivate your account due to privacy concerns.";
            case ACCOUNT_CLEANUP -> "You've requested to deactivate your account as part of cleanup.";
            case SWITCHING_ACCOUNTS -> "You've requested to deactivate your account for account switching.";
            case WORK_REQUIREMENTS -> "You've requested to deactivate your account due to work requirements.";
            case NOT_USEFUL -> "You've requested to deactivate your account as it's no longer useful.";
            case GDPR_REQUEST -> "You've requested permanent deletion of your account under GDPR.";
            case OTHER -> "You've requested to deactivate your account.";
        };

        // Use enum field to determine if reactivation info should be added
        if (reason.allowsReactivation()) {
            baseMessage += " You can reactivate it at any time if you change your mind.";
        } else {
            baseMessage += " Please note that this action cannot be reversed.";
        }

        return baseMessage;
    }

    /**
     * Returns an appropriate subject line for deactivation confirmation emails.
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
            case GDPR_REQUEST -> "GDPR Compliance – Account Permanently Deleted – MyFavApp";
            case OTHER -> "Account Deactivation Confirmed – MyFavApp";
        };
    }

    /**
     * Returns subject line for deactivation request emails.
     * Used when user initially requests deactivation (before confirmation).
     *
     * @param reason The deactivation reason
     * @return Appropriate subject line for the request email
     */
    public String getDeactivationRequestSubjectLine(DeactivationReason reason) {
        if (reason == null) {
            return "Confirm Account Deactivation – MyFavApp";
        }

        return switch (reason) {
            case TAKING_A_BREAK -> "Confirm Break Request – MyFavApp";
            case TOO_MUCH_TIME -> "Confirm Digital Wellness Break – MyFavApp";
            case PRIVACY_CONCERNS -> "Confirm Privacy-Related Deactivation – MyFavApp";
            case ACCOUNT_CLEANUP -> "Confirm Account Cleanup – MyFavApp";
            case SWITCHING_ACCOUNTS -> "Confirm Account Switch – MyFavApp";
            case WORK_REQUIREMENTS -> "Confirm Work-Related Deactivation – MyFavApp";
            case NOT_USEFUL -> "Confirm Account Deactivation – MyFavApp";
            case GDPR_REQUEST -> "Confirm GDPR Deletion Request – MyFavApp";
            case OTHER -> "Confirm Account Deactivation – MyFavApp";
        };
    }

    /**
     * Returns a reactivation welcome message based on the original deactivation reason.
     * Used when user successfully reactivates their account.
     *
     * @param originalReason The original reason for deactivation
     * @return A personalized welcome back message
     */
    public String getReactivationWelcomeMessage(DeactivationReason originalReason) {
        if (originalReason == null) {
            return "Welcome back! Your account has been successfully reactivated.";
        }

        return switch (originalReason) {
            case TAKING_A_BREAK ->
                    "Welcome back! We hope your break was refreshing. Your account is now active and ready to use.";
            case TOO_MUCH_TIME ->
                    "Welcome back! We're glad you've found a good balance. Your account has been reactivated.";
            case PRIVACY_CONCERNS ->
                    "Welcome back! We've continued improving our privacy measures. Your account is now active.";
            case WORK_REQUIREMENTS ->
                    "Welcome back! We hope your work situation has improved. Your account is ready to use.";
            default ->
                    "Welcome back! Your account has been successfully reactivated and is ready to use.";
        };
    }

    /**
     * Logs reactivation eligibility information for audit purposes.
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
}