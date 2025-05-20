package be.steby.CoreProject.bll.domains.password.events;

import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.dl.entities.User;

public record RequestPasswordTokenEvent(
        User user,
        String newToken,
        RequestContext requestContext
) {
}
