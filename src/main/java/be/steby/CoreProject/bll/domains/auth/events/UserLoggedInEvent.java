package be.steby.CoreProject.bll.domains.auth.events;

import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;

public record UserLoggedInEvent(
        User user,
        Device device,
        boolean successful,
        String failureReason,
        RequestContext requestContext
        ) {}
