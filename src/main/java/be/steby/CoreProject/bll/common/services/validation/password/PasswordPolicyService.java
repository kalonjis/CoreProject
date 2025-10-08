package be.steby.CoreProject.bll.common.services.validation.password;

import be.steby.CoreProject.bll.domains.password.models.PasswordValidationResult;

/**
 * Service for password policy validation.
 *
 * ✅ SINGLE RESPONSIBILITY: This service is now focused ONLY on validation.
 * Password generation has been extracted to TemporaryPasswordGeneratorService.
 *
 * Validates passwords against configured policy rules:
 * - Minimum and maximum length
 * - Uppercase letter requirement
 * - Lowercase letter requirement
 * - Digit requirement
 * - Special character requirement
 *
 * All rules are configured in application.properties.
 *
 * @author Steby Core Team
 * @version 2.0 (Refactored - validation only)
 * @since 2025-01
 */
public interface PasswordPolicyService {

    /**
     * Validates a password against all configured policy rules.
     *
     * Checks:
     * - Minimum and maximum length
     * - Uppercase letter requirement
     * - Lowercase letter requirement
     * - Digit requirement
     * - Special character requirement
     *
     * @param password The password to validate
     * @return PasswordValidationResult with validation status and error messages
     */
    PasswordValidationResult validatePassword(String password);
}