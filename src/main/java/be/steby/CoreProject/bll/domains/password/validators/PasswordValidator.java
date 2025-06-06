package be.steby.CoreProject.bll.domains.password.validators;

import be.steby.CoreProject.bll.common.validators.ValidatorResult;

public interface PasswordValidator {

    /**
     * Validates a password according to business rules
     * @param password The password to validate
     * @return The result of the validation
     */
    ValidatorResult validate(String password);
}
