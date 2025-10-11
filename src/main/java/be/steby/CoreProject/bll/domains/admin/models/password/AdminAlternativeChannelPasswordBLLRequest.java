package be.steby.CoreProject.bll.domains.admin.models.password;

/**
 * BLL request for sending temporary password via alternative channel.
 * 
 * Uses flags to indicate which pre-registered channels to use.
 * The actual email/phone values are retrieved from the user profile.
 */
public record AdminAlternativeChannelPasswordBLLRequest(
        String reason,
        boolean useAlternativeEmail,
        boolean useAlternativePhone
) {
    public AdminAlternativeChannelPasswordBLLRequest {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason is required");
        }
        if (reason.length() < 10 || reason.length() > 500) {
            throw new IllegalArgumentException("Reason must be between 10 and 500 characters");
        }

        if (!useAlternativeEmail && !useAlternativePhone) {
            throw new IllegalArgumentException(
                "At least one alternative channel must be selected");
        }
    }

    public String getDeliveryMethod() {
        if (useAlternativeEmail && useAlternativePhone) {
            return "EMAIL_AND_SMS";
        } else if (useAlternativeEmail) {
            return "EMAIL";
        } else {
            return "SMS";
        }
    }
}