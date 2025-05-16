package be.steby.CoreProject.bll.domain.password.services;

import be.steby.CoreProject.pl.security.models.ChangePasswordForm;
import be.steby.CoreProject.pl.security.models.PasswordResetForm;
import jakarta.servlet.http.HttpServletRequest;

public interface PasswordService {
    void resetPassword(PasswordResetForm form, String password, HttpServletRequest request);

    void changePassword(ChangePasswordForm form, HttpServletRequest request);

    void requestPasswordReset(String email, HttpServletRequest request);

    void requestPasswordToken(String token);
}
