package be.steby.CoreProject.bll.domains.emailAddress.events;

import be.steby.CoreProject.dl.entities.User;

public record EmailChangeVerificationEvent(
        User user,
        String token,
        String email
) {
}
