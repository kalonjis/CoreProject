package be.steby.CoreProject.bll.domains.auth.models;

import be.steby.CoreProject.dl.enums.TwoFactorType;

/**
 * Information about current 2FA session
 */
public record TwoFactorSessionInfo(
    String userId,
    String username,
    TwoFactorType twoFactorType,
    String maskedEmail,
    long timeRemainingSeconds,
    int attemptsRemaining
) {}
