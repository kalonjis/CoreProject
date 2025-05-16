package be.steby.CoreProject.bll.events.security.email_events;

import be.steby.CoreProject.bll.models.RequestContext;
import be.steby.CoreProject.dl.entities.User;

public record ChangeEmailVerificationEvent(
        User user,
        RequestContext requestContext,
        String token,
        String email
) {
}
