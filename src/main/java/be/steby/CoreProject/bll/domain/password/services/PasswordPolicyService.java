package be.steby.CoreProject.bll.domain.password.services;

import be.steby.CoreProject.bll.domain.password.models.PasswordValidationResult;

public interface PasswordPolicyService {

    PasswordValidationResult validatePassword(String password);

    String generateSecurePassword();
}
