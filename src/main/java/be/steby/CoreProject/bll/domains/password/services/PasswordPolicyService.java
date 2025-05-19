package be.steby.CoreProject.bll.domains.password.services;

import be.steby.CoreProject.bll.domains.password.models.PasswordValidationResult;

public interface PasswordPolicyService {

    PasswordValidationResult validatePassword(String password);

    String generateSecurePassword();
}
