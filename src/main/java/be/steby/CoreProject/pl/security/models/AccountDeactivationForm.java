package be.steby.CoreProject.pl.security.models;

import be.steby.CoreProject.bll.domains.account.models.DeactivationRequest;
import be.steby.CoreProject.dl.enums.DeactivationReason;
import jakarta.validation.constraints.NotNull;


public record AccountDeactivationForm(
        @NotNull
        DeactivationReason deactivationReason,
        String reasonDetails
) {
    public DeactivationRequest toBusiness(){
        return new DeactivationRequest(deactivationReason(), reasonDetails());
    }
}
