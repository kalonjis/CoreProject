package be.steby.CoreProject.bll.events.account;

import be.steby.CoreProject.bll.models.RequestContext;
import be.steby.CoreProject.dl.entities.User;

public record SignupEvent(
        User user,
        RequestContext requestContext
) {
}
