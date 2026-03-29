package be.steby.CoreProject.dl.enums.crm;

/**
 * Identifies the type of CRM entity targeted by a {@code CrmChangeLog} entry.
 *
 * <p>Used as a discriminator in the {@code crm_change_log} table so that
 * a single table covers all CRM domains without separate per-entity tables.</p>
 */
public enum CrmEntityType {

    /** A CRM contact record. */
    CONTACT,

    /** A sales deal record. */
    DEAL,

    /** A sales lead record. */
    LEAD,

    /** A company or organisation record. */
    ORGANISATION
}
