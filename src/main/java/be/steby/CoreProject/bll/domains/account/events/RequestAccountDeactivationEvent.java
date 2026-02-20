package be.steby.CoreProject.bll.domains.account.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.DeactivationReason;

public record RequestAccountDeactivationEvent(
        User user,
        String token,
        DeactivationReason deactivationReason,
        String reasonDetails,
        Device device ) {
}
