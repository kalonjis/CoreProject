package be.steby.CoreProject.bll.domain.emailAddress.events;

import be.steby.CoreProject.bll.models.RequestContext;
import be.steby.CoreProject.dl.entities.User;

public record EmailChangeRequestEvent(
        User user,
        String email,
        String token,
        RequestContext requestContext
) {
}
