package be.steby.CoreProject.bll.domains.account.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.DeactivationReason;

public record RequestAccountActivationEvent(
        User user,
        String token,
        Device device ) {
}
