package be.steby.CoreProject.bll.domains.emailaddress.validators;

import be.steby.CoreProject.bll.common.validators.ValidatorResult;

public interface EmailValidator {

    /**
     * Validates an email address according to business rules
     *
     * @param email The email address to validate
     * @return The validation result
     */
    ValidatorResult validate(String email);
}
