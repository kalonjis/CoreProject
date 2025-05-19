package be.steby.CoreProject.bll.domains.emailAddress.events;

import be.steby.CoreProject.bll.models.RequestContext;
import be.steby.CoreProject.dl.entities.User;

public record EmailChangeCancellationEvent(
        User user,
        String newEmailAddress,
        RequestContext requestContext
) {

}
