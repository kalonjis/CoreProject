package be.steby.CoreProject.pl.domains.deal.models.responses;

import be.steby.CoreProject.dl.entities.crm.Deal;
import be.steby.CoreProject.dl.enums.crm.DealStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lightweight response model for deal list views.
 *
 * <p>Used in paginated lists — contains only the fields needed
 * to display a row or card in the CRM deal list / Kanban view.
 * For full details, see {@link DealDetailResponse}.</p>
 *
 * @param publicId           public UUID of the deal
 * @param title              short descriptive title
 * @param amount             estimated contract value, or {@code null} if not set
 * @param currency           ISO 4217 currency code
 * @param status             current lifecycle status
 * @param stagePublicId      public UUID of the current pipeline step
 * @param stageName          display name of the current pipeline step
 * @param contactPublicId    public UUID of the primary contact
 * @param contactFullName    full name of the primary contact
 * @param assignedToPublicId public UUID of the assigned commercial
 * @param assignedToUsername username of the assigned commercial
 * @param isOverdue          {@code true} if the deal is open and past its expected close date
 * @param expectedCloseDate  expected closing date, or {@code null} if not set
 */
public record DealSummaryResponse(
        String publicId,
        String title,
        BigDecimal amount,
        String currency,
        DealStatus status,
        String stagePublicId,
        String stageName,
        String contactPublicId,
        String contactFullName,
        String assignedToPublicId,
        String assignedToUsername,
        boolean isOverdue,
        LocalDate expectedCloseDate
) {

    /**
     * Maps a {@link Deal} entity to a {@link DealSummaryResponse}.
     *
     * @param deal the deal entity
     * @return the summary response
     */
    public static DealSummaryResponse fromEntity(Deal deal) {
        return new DealSummaryResponse(
                deal.getPublicId(),
                deal.getTitle(),
                deal.getAmount(),
                deal.getCurrency(),
                deal.getStatus(),
                deal.getPipelineStep() != null ? deal.getPipelineStep().getPublicId() : null,
                deal.getPipelineStep() != null ? deal.getPipelineStep().getName()     : null,
                deal.getContact()      != null ? deal.getContact().getPublicId()      : null,
                deal.getContact()      != null ? deal.getContact().getFullName()      : null,
                deal.getAssignedTo()   != null ? deal.getAssignedTo().getPublicId()  : null,
                deal.getAssignedTo()   != null ? deal.getAssignedTo().getUsername()  : null,
                deal.isOverdue(),
                deal.getExpectedCloseDate()
        );
    }
}
