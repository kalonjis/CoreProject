package be.steby.CoreProject.bll.domains.admin.events.password;

import be.steby.CoreProject.dl.entities.User;

public record AdminAlternativeChannelPasswordEvent(
    User target,
    User admin,
    String temporaryPassword,
    String reason,
    String alternativeEmail,
    String alternativePhone,
    String deliveryMethod
) {}