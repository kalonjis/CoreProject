package be.steby.CoreProject.bll.domains.auth.events;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.TwoFactorType;

/**
 * Event published when a user enables a two-factor authentication method.
 * Triggers confirmation email sending via event listener.
 * 
 * @author Steby Team
 * @since 2.0.0
 */
public record TwoFactorEnabledEvent(
        User user,
        TwoFactorType type
) {
}