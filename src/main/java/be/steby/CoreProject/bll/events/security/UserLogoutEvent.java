package be.steby.CoreProject.bll.events.security;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import jakarta.servlet.http.HttpServletRequest;

public record UserLogoutEvent(
        User user,
        Device device,
        HttpServletRequest request
) {
}
