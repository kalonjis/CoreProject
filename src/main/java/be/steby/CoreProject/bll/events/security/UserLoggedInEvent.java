package be.steby.CoreProject.bll.events.security;

import be.steby.CoreProject.bll.models.RequestContext;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;

public record UserLoggedInEvent(
        User user,
        Device device,
        boolean successful,
        String failureReason,
        RequestContext request
        ) {}
