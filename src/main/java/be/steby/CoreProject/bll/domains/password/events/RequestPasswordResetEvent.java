package be.steby.CoreProject.bll.domains.password.events;

import be.steby.CoreProject.bll.models.RequestContext;
import be.steby.CoreProject.dl.entities.User;

public record RequestPasswordResetEvent(
        User user,
        String token,
        RequestContext requestContext) {
}
