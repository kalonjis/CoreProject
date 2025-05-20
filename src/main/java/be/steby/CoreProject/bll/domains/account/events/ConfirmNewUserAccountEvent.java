package be.steby.CoreProject.bll.domains.account.events;

import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.dl.entities.User;

public record ConfirmNewUserAccountEvent(
        User user,
        RequestContext requestContext
        ) { }
