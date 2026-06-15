package be.steby.CoreProject.pl.domains.call.models.responses;

import be.steby.CoreProject.bll.domains.crm.call.models.CallerInfo;

/**
 * Response for {@code GET /api/crm/calls/caller-info?number=}.
 *
 * @param displayName     human-readable name to show in the call widget; {@code null} if unknown
 * @param contactPublicId public UUID of the matching CRM contact; {@code null} for internal/unknown callers
 * @param isInternalUser  {@code true} when the number matches a SIP extension of a platform user
 */
public record CallerInfoResponse(
        String displayName,
        String contactPublicId,
        boolean isInternalUser
) {
    public static CallerInfoResponse from(CallerInfo info) {
        return new CallerInfoResponse(info.displayName(), info.contactPublicId(), info.isInternalUser());
    }
}
