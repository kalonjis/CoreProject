package be.steby.CoreProject.dl.enums.crm;

/**
 * Acquisition source of a lead — how the prospect found us.
 *
 * <p>Set automatically from UTM parameters on the public contact form,
 * or manually corrected by the commercial during enrichment.</p>
 */
public enum LeadSource {

    /** Submitted via the public contact form (no UTM params detected). */
    CONTACT_FORM,

    /** Inbound phone call handled manually by the team. */
    PHONE,

    /** Inbound email not going through the contact form. */
    EMAIL,

    /** Referred by an existing client or partner. */
    REFERRAL,

    /** Came from a social media post or profile (organic). */
    SOCIAL_MEDIA,

    /** Came from a paid advertising campaign (utm_medium=cpc/paid). */
    PAID_CAMPAIGN,

    /** Came from an organic search engine result. */
    ORGANIC_SEARCH,

    /** Met at a physical or virtual event. */
    EVENT,

    /** Manually entered by the commercial team (no web submission). */
    MANUAL,

    /** Other or unknown source. */
    OTHER
}
