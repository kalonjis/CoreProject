package be.steby.CoreProject.pl.domains.deal.models.responses;

import be.steby.CoreProject.dl.entities.crm.Contact;
import be.steby.CoreProject.dl.entities.crm.Deal;
import be.steby.CoreProject.dl.enums.crm.DealStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Full response model for the deal detail view.
 *
 * <p>{@code contactPublicId} and {@code contactFullName} reflect the primary
 * contact for quick display. The full list of involved contacts (with roles)
 * is available in {@code contacts}.</p>
 *
 * @param publicId             public UUID of the deal
 * @param title                short descriptive title
 * @param amount               estimated contract value, or {@code null} if not set
 * @param currency             ISO 4217 currency code
 * @param status               current lifecycle status
 * @param expectedCloseDate    expected closing date, or {@code null} if not set
 * @param closedAt             actual closing timestamp, or {@code null} if still open
 * @param pipelinePublicId     public UUID of the deal's pipeline
 * @param pipelineName         display name of the deal's pipeline
 * @param stagePublicId        public UUID of the current pipeline step
 * @param stageName            display name of the current pipeline step
 * @param isOverdue            {@code true} if the deal is open and past its expected close date
 * @param contactPublicId      public UUID of the primary contact
 * @param contactFullName      full name of the primary contact
 * @param contacts             all contacts involved in this deal, with their roles
 * @param organisationPublicId public UUID of the linked organisation, or {@code null}
 * @param organisationName     display name of the linked organisation, or {@code null}
 * @param assignedToPublicId   public UUID of the assigned commercial
 * @param assignedToUsername   username of the assigned commercial
 * @param notes                internal notes, or {@code null} if none
 * @param createdAt            timestamp of entity creation
 * @param updatedAt            timestamp of last update
 */
public record DealDetailResponse(
        String publicId,
        String title,
        BigDecimal amount,
        String currency,
        DealStatus status,
        LocalDate expectedCloseDate,
        Instant closedAt,
        String pipelinePublicId,
        String pipelineName,
        String stagePublicId,
        String stageName,
        boolean isOverdue,
        String contactPublicId,
        String contactFullName,
        List<DealContactRoleResponse> contacts,
        String organisationPublicId,
        String organisationName,
        String assignedToPublicId,
        String assignedToUsername,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {

    public static DealDetailResponse fromEntity(Deal deal) {
        Contact primary = deal.getPrimaryContact();

        List<DealContactRoleResponse> contacts = deal.getContactRoles().stream()
                .map(DealContactRoleResponse::fromEntity)
                .toList();

        return new DealDetailResponse(
                deal.getPublicId(),
                deal.getTitle(),
                deal.getAmount(),
                deal.getCurrency(),
                deal.getStatus(),
                deal.getExpectedCloseDate(),
                deal.getClosedAt(),
                deal.getPipeline()     != null ? deal.getPipeline().getPublicId()     : null,
                deal.getPipeline()     != null ? deal.getPipeline().getName()          : null,
                deal.getPipelineStep() != null ? deal.getPipelineStep().getPublicId() : null,
                deal.getPipelineStep() != null ? deal.getPipelineStep().getName()     : null,
                deal.isOverdue(),
                primary != null ? primary.getPublicId()  : null,
                primary != null ? primary.getFullName()  : null,
                contacts,
                deal.getOrganisation() != null ? deal.getOrganisation().getPublicId()  : null,
                deal.getOrganisation() != null ? deal.getOrganisation().getName()      : null,
                deal.getAssignedTo()   != null ? deal.getAssignedTo().getPublicId()   : null,
                deal.getAssignedTo()   != null ? deal.getAssignedTo().getUsername()   : null,
                deal.getNotes(),
                deal.getCreatedAt(),
                deal.getUpdatedAt()
        );
    }
}
