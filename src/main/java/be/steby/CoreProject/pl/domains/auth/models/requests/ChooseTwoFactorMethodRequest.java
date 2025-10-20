package be.steby.CoreProject.pl.domains.auth.models.requests;

import be.steby.CoreProject.dl.enums.TwoFactorType;
import jakarta.validation.constraints.NotNull;

/**
 * Request to choose a specific two-factor authentication method during login flow.
 * 
 * User selects which 2FA method to use from their available enabled methods.
 * This triggers the appropriate verification process for the chosen method.
 * 
 * @param twoFactorType the chosen 2FA method type
 */
public record ChooseTwoFactorMethodRequest(
        @NotNull(message = "Two-factor type is required")
        TwoFactorType twoFactorType
) {}