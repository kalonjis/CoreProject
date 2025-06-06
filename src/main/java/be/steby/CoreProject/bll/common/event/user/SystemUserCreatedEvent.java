package be.steby.CoreProject.bll.common.event.user;

import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.dl.entities.User;

/**
 * Event published when a user is created by the system
 */
public record SystemUserCreatedEvent(
        User user,
        String temporaryPassword,
        RequestContext requestContext
) {

}
