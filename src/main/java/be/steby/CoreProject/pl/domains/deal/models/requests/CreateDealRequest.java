package be.steby.CoreProject.pl.domains.deal.models.requests;

import be.steby.CoreProject.bll.domains.crm.deal.models.DealCreateRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * PL request model for manually creating a deal.
 *
 * @param title                short descriptive title of the deal (required)
 * @param amount               estimated contract value (optional)
 * @param currency             ISO 4217 currency code, defaults to "EUR" if not provided (optional)
 * @param pipelinePublicId     public UUID of the pipeline (required)
 * @param stagePublicId        public UUID of the initial pipeline step (required)
 * @param contactPublicId      public UUID of the primary contact (required)
 * @param organisationPublicId public UUID of the associated organisation (optional)
 * @param assignedToPublicId   public UUID of the assigned commercial (required)
 * @param expectedCloseDate    expected closing date for forecasting (optional)
 * @param notes                internal notes for the commercial team (optional)
 */
public record CreateDealRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,

        BigDecimal amount,

        @Size(max = 3, message = "Currency code must not exceed 3 characters")
        String currency,

        @NotBlank(message = "Pipeline is required")
        String pipelinePublicId,

        @NotBlank(message = "Stage is required")
        String stagePublicId,

        @NotBlank(message = "Contact is required")
        String contactPublicId,

        String organisationPublicId,

        @NotBlank(message = "Assigned commercial is required")
        String assignedToPublicId,

        LocalDate expectedCloseDate,

        String notes

) {

    /**
     * Converts this PL request to the BLL model.
     *
     * @return {@link DealCreateRequest} for the service layer
     */
    public DealCreateRequest toBllModel() {
        return new DealCreateRequest(
                title.trim(),
                amount,
                currency,
                pipelinePublicId,
                stagePublicId,
                contactPublicId,
                organisationPublicId,
                assignedToPublicId,
                expectedCloseDate,
                notes != null ? notes.trim() : null
        );
    }
}
