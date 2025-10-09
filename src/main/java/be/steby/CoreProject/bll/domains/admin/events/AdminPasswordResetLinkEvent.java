package be.steby.CoreProject.bll.domains.admin.events;

import be.steby.CoreProject.dl.entities.User;

public record AdminPasswordResetLinkEvent(
        User target,
        User admin,
        String tokenPublicId,
        String reason
) {
}
