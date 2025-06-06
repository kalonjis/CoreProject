package be.steby.CoreProject.bll.common.event.user;

import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.dl.entities.User;


/**
 * Event published when a user is created by a (super)admin
 */
public record AdminUserCreatedEvent(
        User user,
        String confirmationToken,
        String temporaryPassword,
        RequestContext requestContext
) {

}
