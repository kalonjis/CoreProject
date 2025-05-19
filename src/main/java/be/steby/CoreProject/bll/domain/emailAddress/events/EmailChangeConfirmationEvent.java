package be.steby.CoreProject.bll.domain.emailAddress.events;

import be.steby.CoreProject.bll.models.RequestContext;
import be.steby.CoreProject.dl.entities.User;

public record EmailChangeConfirmationEvent(
        User user,
        String token,
        String oldAddress,
        String newAddress,
        RequestContext requestContext
) {

}
