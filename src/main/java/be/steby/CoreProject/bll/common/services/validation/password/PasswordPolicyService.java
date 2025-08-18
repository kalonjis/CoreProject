package be.steby.CoreProject.bll.common.services.validation.password;

import be.steby.CoreProject.bll.domains.password.models.PasswordValidationResult;

public interface PasswordPolicyService {

    PasswordValidationResult validatePassword(String password);

    String generateSecurePassword();
}
