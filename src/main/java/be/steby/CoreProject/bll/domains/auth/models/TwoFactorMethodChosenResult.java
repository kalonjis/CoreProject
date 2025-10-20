package be.steby.CoreProject.bll.domains.auth.models;

import be.steby.CoreProject.dl.enums.TwoFactorType;

/**
 * Result object for two-factor method selection operation.
 * 
 * Contains the JWT token and information about the chosen method
 * for the presentation layer to handle appropriately.
 * 
 * @param twoFactorToken JWT token for the verification phase
 * @param chosenMethod the 2FA method that was selected
 * @param codeGenerated whether a verification code was generated and sent
 * @param maskedTarget where the code was sent (if applicable)
 * 
 * @author Steby Team
 * @since 2.0.0
 */
public record TwoFactorMethodChosenResult(
        String twoFactorToken,
        TwoFactorType chosenMethod,
        boolean codeGenerated,
        String maskedTarget
) {}