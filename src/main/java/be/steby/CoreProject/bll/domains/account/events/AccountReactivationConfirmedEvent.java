package be.steby.CoreProject.bll.domains.account.events;

import be.steby.CoreProject.bll.common.models.RequestContext;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.DeactivationReason;

/**
 * Event published when an account deactivation is confirmed and completed
 */
public record AccountReactivationConfirmedEvent(
        User user,
        RequestContext requestContext
) { }