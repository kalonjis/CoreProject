package be.steby.CoreProject.pl.domains.deal.models.requests;

import jakarta.validation.constraints.NotBlank;

/**
 * PL request model for moving a deal to a different pipeline stage.
 *
 * @param stagePublicId public UUID of the target pipeline step (required)
 * @param lostReason    reason why the deal was lost (required when target stage is a lost stage)
 */
public record MoveDealStageRequest(

        @NotBlank(message = "Stage is required")
        String stagePublicId,

        String lostReason

) {}
