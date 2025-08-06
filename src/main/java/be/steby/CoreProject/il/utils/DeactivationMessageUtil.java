package be.steby.CoreProject.il.utils;

import be.steby.CoreProject.dl.enums.DeactivationReason;
import org.springframework.stereotype.Component;

/**
 * Utility class that provides localized messages for account deactivation confirmations
 * based on the deactivation reason.
 */
@Component
public class DeactivationMessageUtil {

    /**
     * Returns an appropriate confirmation message based on the deactivation reason
     *
     * @param reason The reason for deactivation
     * @param reasonDetails Additional details provided by the user (can be null)
     * @return A personalized message for the confirmation email
     */
    public String getConfirmationMessage(DeactivationReason reason, String reasonDetails) {
        if (reason == null) {
            return getDefaultMessage();
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

            case ADMIN_DECISION ->
                    "Your account has been deactivated by our administrative team. This action was taken in accordance " +
                            "with our terms of service and community guidelines. If you believe this action was taken in error " +
                            "or if you have questions about the deactivation, please contact our support team who will be " +
                            "happy to review your case.";

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

    /**
     * Returns a default message when no specific reason is provided
     */
    private String getDefaultMessage() {
        return "Your account has been successfully deactivated. We're sorry to see you go, but we understand " +
                "that everyone's needs are different. Your account can be reactivated at any time if you decide to return. " +
                "Thank you for being part of our community.";
    }

    /**
     * Returns a subject line for the confirmation email based on the deactivation reason
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
            case ADMIN_DECISION -> "Administrative Action – Account Deactivated – MyFavApp";
            case OTHER -> "Account Deactivation Confirmed – MyFavApp";
        };
    }

    /**
     * Returns a personalized message for the deactivation request email based on the reason
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
                            "We take your privacy seriously and understand your decision.";

            case ACCOUNT_CLEANUP ->
                    "We have received your request to deactivate your account as part of your account cleanup process. " +
                            "We understand the importance of managing your digital presence.";

            case SWITCHING_ACCOUNTS ->
                    "We have received your request to deactivate this account as you're switching to another one. " +
                            "We're here to help make this transition as smooth as possible.";

            case WORK_REQUIREMENTS ->
                    "We have received your request to deactivate your account due to work requirements. " +
                            "We understand that professional obligations sometimes require these changes.";

            case NOT_USEFUL ->
                    "We have received your request to deactivate your account as it's no longer useful to you. " +
                            "We're sorry that our application didn't meet your needs.";

            case GDPR_REQUEST ->
                    "We have received your request to deactivate your account and delete your personal data " +
                            "in accordance with GDPR regulations. We will process this request according to " +
                            "European data protection law.";

            case ADMIN_DECISION ->
                    "Your account deactivation has been initiated by our administrative team in accordance " +
                            "with our terms of service and community guidelines.";

            case OTHER -> {
                String baseMessage = "We have received your request to deactivate your account.";
                if (reasonDetails != null && !reasonDetails.trim().isEmpty()) {
                    yield baseMessage + " You mentioned: \"" + reasonDetails.trim() + "\"";
                }
                yield baseMessage;
            }
        };
    }

    /**
     * Returns a subject line for the deactivation request email based on the reason
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
            case ADMIN_DECISION -> "Administrative Action – Account Deactivation – MyFavApp";
            case OTHER -> "Confirm Your Account Deactivation – MyFavApp";
        };
    }

    /**
     * Determines if the deactivation reason allows for account reactivation
     *
     * @param reason The deactivation reason
     * @return true if reactivation is possible, false otherwise
     */
    public boolean isReactivationAllowed(DeactivationReason reason) {
        if (reason == null) {
            return true; // Default allows reactivation
        }

        return switch (reason) {
            case GDPR_REQUEST -> false; // GDPR deletion is permanent
            case ADMIN_DECISION -> false; // Admin decisions require manual review
            default -> true; // All other reasons allow reactivation
        };
    }
}