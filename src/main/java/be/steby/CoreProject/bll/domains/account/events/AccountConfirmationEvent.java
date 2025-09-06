package be.steby.CoreProject.bll.domains.account.events;

import be.steby.CoreProject.dl.entities.User;

public record AccountConfirmationEvent(
        User user) { }
