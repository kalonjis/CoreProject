package be.steby.CoreProject.bll.domains.account.events;

import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.dl.entities.User;

public record RequestAccountDeactivationEvent(
        User user,
        String token,
        RequestContext requestContext
) {
}
