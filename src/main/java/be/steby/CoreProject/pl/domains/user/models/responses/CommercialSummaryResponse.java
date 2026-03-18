package be.steby.CoreProject.pl.domains.user.models.responses;

import be.steby.CoreProject.dl.entities.User;

/**
 * Lightweight projection of a commercial user for CRM assignment dropdowns.
 *
 * @param publicId   the public UUID exposed in API requests
 * @param firstName  first name
 * @param lastName   last name
 * @param username   username (fallback display when name is absent)
 */
public record CommercialSummaryResponse(
        String publicId,
        String firstName,
        String lastName,
        String username
) {
    public static CommercialSummaryResponse from(User user) {
        return new CommercialSummaryResponse(
                user.getPublicId(),
                user.getFirstname(),
                user.getLastname(),
                user.getUsername()
        );
    }
}
