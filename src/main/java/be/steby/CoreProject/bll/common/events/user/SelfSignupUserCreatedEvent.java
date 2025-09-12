package be.steby.CoreProject.bll.common.events.user;

import be.steby.CoreProject.dl.entities.User;


/**
 * Event published when a user self-registers
 */
public record SelfSignupUserCreatedEvent(
        User user,
        String confirmationToken) {

}
