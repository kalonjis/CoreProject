package be.steby.CoreProject.bll.domains.crm.call.models;

/**
 * Resolved identity of an inbound caller.
 *
 * @param displayName      human-readable name to show in the call widget; {@code null} if unknown
 * @param contactPublicId  public UUID of the matching CRM contact; {@code null} for internal/unknown callers
 * @param isInternalUser   {@code true} when the number matches a SIP extension of a platform user
 */
public record CallerInfo(
        String displayName,
        String contactPublicId,
        boolean isInternalUser
) {
    public static CallerInfo unknown() {
        return new CallerInfo(null, null, false);
    }
}
