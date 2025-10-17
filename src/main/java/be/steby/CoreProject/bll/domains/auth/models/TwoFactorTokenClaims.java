package be.steby.CoreProject.bll.domains.auth.models;

import be.steby.CoreProject.dl.enums.TwoFactorType;

/**
 * JWT claims for 2FA token
 */
public record TwoFactorTokenClaims(
    String userId,
    String username,
    String email,
    TwoFactorType twoFactorType,
    String verificationCode, // Hashed verification code
    long issuedAt,
    long expiresAt
) {}