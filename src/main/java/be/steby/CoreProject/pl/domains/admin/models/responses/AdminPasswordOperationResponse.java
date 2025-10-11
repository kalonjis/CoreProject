package be.steby.CoreProject.pl.domains.admin.models.responses;

/**
 * Response for admin-password-related operations.
 */
public record AdminPasswordOperationResponse(
        String message
) {
    public static AdminPasswordOperationResponse resetLinkSent() {
        return new AdminPasswordOperationResponse(
                "An email with a password reset link has been successfully sent"
        );
    }


    public static AdminPasswordOperationResponse temporaryPasswordSent() {
        return new AdminPasswordOperationResponse(
                "An email with a temporary password has been successfully sent"
        );
    }

    public static AdminPasswordOperationResponse alternativeChannelPasswordSent(
            boolean sentToEmail,
            boolean sentToSMS) {

        String channels = "";
        if (sentToEmail && sentToSMS) {
            channels = "alternative email and SMS";
        } else if (sentToEmail) {
            channels = "alternative email";
        } else {
            channels = "SMS";
        }

        return new AdminPasswordOperationResponse(
                String.format(
                        "A temporary password has been generated and sent via %s. " +
                            "The user must change this password on next login. " +
                                "All active sessions have been terminated.",
                        channels
                )
        );
    }

//    public static AdminPasswordOperationResponse accountActivated() {
//        return new AdminPasswordOperationResponse("Account activated successfully");
//    }
//
//    public static AdminPasswordOperationResponse activationRequested() {
//        return new AdminPasswordOperationResponse(
//                "If your request was valid, a new activation link has been sent to your email."
//        );
//    }
//
//    public static AdminPasswordOperationResponse deactivationRequested() {
//        return new AdminPasswordOperationResponse(
//                "A confirmation email has been sent. Please check your inbox."
//        );
//    }
//
//    public static AdminPasswordOperationResponse accountDeactivated() {
//        return new AdminPasswordOperationResponse("Account deactivated successfully");
//    }
//
//    public static AdminPasswordOperationResponse reactivationRequested() {
//        return new AdminPasswordOperationResponse(
//                "A confirmation email has been sent. Please check your inbox."
//        );
//    }
//
//    public static AdminPasswordOperationResponse accountReactivated() {
//        return new AdminPasswordOperationResponse("Account reactivated successfully");
//    }
}