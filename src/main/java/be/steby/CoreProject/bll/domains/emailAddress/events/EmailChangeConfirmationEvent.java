package be.steby.CoreProject.bll.domains.emailAddress.events;

import be.steby.CoreProject.dl.entities.User;

public record EmailChangeConfirmationEvent(
        User user,
        String token,
        String oldAddress,
        String newAddress
        ) { }
