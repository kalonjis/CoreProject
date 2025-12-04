//package be.steby.CoreProject.bll.domains.password.models;
//
///**
// * Result of SMS password reset initiation.
// *
// * <p>Contains the JWT token and success status after initiating SMS password reset.
// * This allows the presentation layer to set the appropriate cookie with the verification token.
// *
// * <p>Pattern similar to PhoneVerificationTokenResult but specific to password reset flow.
// *
// * @param success whether the SMS password reset was initiated successfully
// * @param jwtToken JWT token containing hashed verification code (null if failed)
// */
//public record SmsPasswordResetResult(
//        boolean success,
//        String jwtToken
//) {
//
//    /**
//     * Creates a successful result with JWT token.
//     *
//     * @param jwtToken the JWT token to be stored in cookie
//     * @return successful SMS password reset result
//     */
//    public static SmsPasswordResetResult success(String jwtToken) {
//        return new SmsPasswordResetResult(true, jwtToken);
//    }
//
//    /**
//     * Creates a failed result (no JWT token).
//     * Used when SMS requirements are not met or other failures occur.
//     *
//     * @return failed SMS password reset result
//     */
//    public static SmsPasswordResetResult failure() {
//        return new SmsPasswordResetResult(false, null);
//    }
//}