package be.steby.CoreProject.pl.models.account;

import be.steby.CoreProject.bll.domains.account.models.DeactivationRequest;
import be.steby.CoreProject.dl.enums.DeactivationReason;
import be.steby.CoreProject.pl.validators.account.ValidDeactivationRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


@ValidDeactivationRequest
public record AccountDeactivationForm(
        @NotNull
        DeactivationReason deactivationReason,

        @Size(max = 500, message = "Details may not exceed 500 characters")
        String reasonDetails
) {
    public DeactivationRequest toBusiness(){
        return new DeactivationRequest(deactivationReason(), reasonDetails());
    }
}
