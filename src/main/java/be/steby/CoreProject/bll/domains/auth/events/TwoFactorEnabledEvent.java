package be.steby.CoreProject.bll.domains.auth.events;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.TwoFactorType;

/**
 * Event published when a user enables a two-factor authentication method.
 * Used by listeners to send confirmation emails and perform other actions.
 */
public record TwoFactorEnabledEvent(User user, TwoFactorType type) {}