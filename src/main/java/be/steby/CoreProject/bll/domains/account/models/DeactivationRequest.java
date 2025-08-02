package be.steby.CoreProject.bll.domains.account.models;

import be.steby.CoreProject.dl.enums.DeactivationReason;

public record DeactivationRequest(

        DeactivationReason deactivationReason,
        String reasonDetails
) {
}
