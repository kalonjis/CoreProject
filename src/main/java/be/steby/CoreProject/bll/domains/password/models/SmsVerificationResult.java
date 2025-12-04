//package be.steby.CoreProject.bll.domains.password.models;
//
///**
// * Result of SMS password reset code verification.
// *
// * <p>Contains the verification status and optionally a permission token
// * that grants access to the password reset page.
// *
// * @param success whether the SMS code verification was successful
// * @param resetPermissionToken JWT token that grants permission to reset password (null if failed)
// */
//public record SmsVerificationResult(
//        boolean success,
//        String resetPermissionToken
//) {
//
//    /**
//     * Creates a successful verification result with permission token.
//     *
//     * @param resetPermissionToken the permission token for password reset page
//     * @return successful SMS verification result
//     */
//    public static SmsVerificationResult success(String resetPermissionToken) {
//        return new SmsVerificationResult(true, resetPermissionToken);
//    }
//
//    /**
//     * Creates a failed verification result.
//     *
//     * @return failed SMS verification result
//     */
//    public static SmsVerificationResult failure() {
//        return new SmsVerificationResult(false, null);
//    }
//}