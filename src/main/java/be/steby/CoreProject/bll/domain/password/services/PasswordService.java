package be.steby.CoreProject.bll.domain.password.services;

import be.steby.CoreProject.bll.domain.password.models.PasswordChangeRequest;
import be.steby.CoreProject.bll.domain.password.models.PasswordResetRequest;
import be.steby.CoreProject.pl.security.models.ChangePasswordForm;
import be.steby.CoreProject.pl.security.models.PasswordResetForm;
import jakarta.servlet.http.HttpServletRequest;

public interface PasswordService {
    void resetPassword(PasswordResetRequest request, String password, HttpServletRequest httpRequest);

    void changePassword(PasswordChangeRequest request, HttpServletRequest httpRequest);

    void requestPasswordReset(String email, HttpServletRequest request);

    void requestPasswordToken(String token, HttpServletRequest httpRequest);
}
