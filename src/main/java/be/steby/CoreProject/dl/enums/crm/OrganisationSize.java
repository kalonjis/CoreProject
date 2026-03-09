package be.steby.CoreProject.dl.enums.crm;

/**
 * Represents the approximate headcount size of an {@link be.steby.CoreProject.dl.entities.crm.Organisation}.
 *
 * <p>Used for lead qualification and deal sizing. Buckets follow a common
 * B2B segmentation model (SMB / Mid-market / Enterprise).</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * organisation.setSize(OrganisationSize.SMALL);
 * }</pre>
 */
public enum OrganisationSize {

    /** 1 to 10 employees — micro-business or sole trader. */
    MICRO,

    /** 11 to 50 employees — small business. */
    SMALL,

    /** 51 to 250 employees — medium-sized business. */
    MEDIUM,

    /** 251 to 1 000 employees — large business. */
    LARGE,

    /** More than 1 000 employees — enterprise / corporate. */
    ENTERPRISE
}