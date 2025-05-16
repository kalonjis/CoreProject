package be.steby.CoreProject.bll.events.security.email_events;

import be.steby.CoreProject.bll.models.RequestContext;
import be.steby.CoreProject.dl.entities.User;

public record ChangeEmailRequestEvent(
        User user,
        String email,
        String token,
        RequestContext requestContext
) {
}
