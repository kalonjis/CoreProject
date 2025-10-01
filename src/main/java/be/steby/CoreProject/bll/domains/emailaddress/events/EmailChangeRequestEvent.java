package be.steby.CoreProject.bll.domains.emailaddress.events;

import be.steby.CoreProject.dl.entities.User;

public record EmailChangeRequestEvent(
        User user,
        String email,
        String token
) {
}
