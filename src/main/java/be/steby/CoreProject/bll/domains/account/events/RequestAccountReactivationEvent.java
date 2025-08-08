package be.steby.CoreProject.bll.domains.account.events;

import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.DeactivationReason;

public record RequestAccountReactivationEvent(
        User user,
        String token,
        RequestContext requestContext
) {
}
