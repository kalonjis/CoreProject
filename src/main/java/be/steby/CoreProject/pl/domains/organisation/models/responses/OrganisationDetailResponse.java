package be.steby.CoreProject.pl.domains.organisation.models.responses;

import be.steby.CoreProject.dl.entities.crm.Organisation;
import be.steby.CoreProject.dl.enums.crm.OrganisationSize;
import be.steby.CoreProject.pl.domains.tag.models.responses.TagResponse;

import java.time.Instant;
import java.util.List;

/**
 * Full response model for the organisation detail view.
 *
 * <p>Includes all fields available on an organisation.
 * For list views, use {@link OrganisationSummaryResponse} instead.</p>
 *
 * @param publicId        public UUID of the organisation
 * @param name            legal or commercial name
 * @param website         public website URL, or {@code null} if not set
 * @param industry        sector of activity, or {@code null} if not set
 * @param size            approximate headcount bucket, or {@code null} if not set
 * @param phone           main switchboard phone number, or {@code null} if not set
 * @param addressPublicId public UUID of the linked address, or {@code null} if none
 * @param notes           internal notes, or {@code null} if none
 * @param tags            tags applied to this organisation
 * @param createdAt       timestamp of entity creation
 * @param updatedAt       timestamp of last update
 */
public record OrganisationDetailResponse(
        String publicId,
        String name,
        String website,
        String industry,
        OrganisationSize size,
        String phone,
        String addressPublicId,
        String notes,
        List<TagResponse> tags,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Maps an {@link Organisation} entity to an {@link OrganisationDetailResponse}.
     *
     * @param organisation the organisation entity
     * @return the detail response
     */
    public static OrganisationDetailResponse fromEntity(Organisation organisation) {
        return new OrganisationDetailResponse(
                organisation.getPublicId(),
                organisation.getName(),
                organisation.getWebsite(),
                organisation.getIndustry(),
                organisation.getSize(),
                organisation.getPhone(),
                organisation.getAddress() != null ? organisation.getAddress().getPublicId() : null,
                organisation.getNotes(),
                organisation.getTags().stream().map(TagResponse::fromEntity).toList(),
                organisation.getCreatedAt(),
                organisation.getUpdatedAt()
        );
    }
}
