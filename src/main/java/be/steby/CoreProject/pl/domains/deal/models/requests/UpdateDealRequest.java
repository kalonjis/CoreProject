package be.steby.CoreProject.pl.domains.deal.models.requests;

import be.steby.CoreProject.bll.domains.deal.models.DealUpdateRequest;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * PL request model for partially updating an existing deal.
 *
 * <p>All fields are optional — only non-null values are applied by the service layer.
 * Null fields are ignored and existing values are preserved.</p>
 *
 * @param title             new title for the deal (optional)
 * @param amount            new estimated contract value (optional)
 * @param currency          new ISO 4217 currency code (optional)
 * @param expectedCloseDate new expected closing date (optional)
 * @param notes             updated internal notes (optional)
 */
public record UpdateDealRequest(

        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,

        BigDecimal amount,

        @Size(max = 3, message = "Currency code must not exceed 3 characters")
        String currency,

        LocalDate expectedCloseDate,

        String notes

) {

    /**
     * Converts this PL request to the BLL model.
     *
     * @return {@link DealUpdateRequest} for the service layer
     */
    public DealUpdateRequest toBllModel() {
        return new DealUpdateRequest(
                title    != null ? title.trim()    : null,
                amount,
                currency,
                expectedCloseDate,
                notes    != null ? notes.trim()    : null
        );
    }
}
