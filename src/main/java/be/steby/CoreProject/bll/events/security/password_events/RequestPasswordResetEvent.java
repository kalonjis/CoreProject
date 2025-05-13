package be.steby.CoreProject.bll.events.security.password_events;

import be.steby.CoreProject.bll.models.RequestContext;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;

public record RequestPasswordResetEvent(
        User user,
        Device device,
        RequestContext requestContext) {
}
