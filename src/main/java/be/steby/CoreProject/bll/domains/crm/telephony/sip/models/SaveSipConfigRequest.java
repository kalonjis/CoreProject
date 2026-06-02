package be.steby.CoreProject.bll.domains.crm.telephony.sip.models;

/**
 * BLL model for creating or updating a {@link be.steby.CoreProject.dl.entities.crm.CommercialSipConfig}.
 *
 * @param targetUserPublicId public UUID of the commercial to configure
 * @param sipUsername        SIP extension username on Asterisk
 * @param sipPassword        plain-text SIP password (encrypted before persistence)
 * @param displayName        optional SIP display name; defaults to commercial's full name when null
 */
public record SaveSipConfigRequest(
        String targetUserPublicId,
        String sipUsername,
        String sipPassword,
        String displayName
) {}
