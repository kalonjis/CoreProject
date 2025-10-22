package be.steby.CoreProject.bll.domains.profile.models.phone;

public record PhoneVerificationRequestBLL(
        String verificationCode,
        String jwtToken
) {
}
