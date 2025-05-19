package be.steby.CoreProject.bll.domains.password.services;

import be.steby.CoreProject.bll.domains.password.models.PasswordChangeRequest;
import be.steby.CoreProject.bll.domains.password.models.PasswordResetRequest;
import jakarta.servlet.http.HttpServletRequest;

public interface PasswordService {
    void resetPassword(PasswordResetRequest request, String password, HttpServletRequest httpRequest);

    void changePassword(PasswordChangeRequest request, HttpServletRequest httpRequest);

    void requestPasswordReset(String email, HttpServletRequest request);

    void requestPasswordToken(String token, HttpServletRequest httpRequest);
}
