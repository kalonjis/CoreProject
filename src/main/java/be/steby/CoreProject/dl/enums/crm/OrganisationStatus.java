package be.steby.CoreProject.dl.enums.crm;

/**
 * Lifecycle status of an {@link be.steby.CoreProject.dl.entities.crm.Organisation}
 * in the CRM relationship pipeline.
 *
 * <p>Follows the industry-standard HubSpot Company lifecycle model:</p>
 * <ul>
 *   <li>{@link #PROSPECT} — default status; an organisation we are actively pursuing</li>
 *   <li>{@link #CLIENT}   — at least one deal WON with this organisation</li>
 * </ul>
 *
 * <p>Transition: {@code PROSPECT → CLIENT} is triggered automatically by
 * {@link be.steby.CoreProject.bll.domains.contact.listeners.DealWonContactUpgradeListener}
 * when a deal linked to this organisation passes to a WON stage.</p>
 */
public enum OrganisationStatus {

    /** Default status — organisation is a prospect, no WON deal yet. */
    PROSPECT,

    /** At least one deal with this organisation has been WON. */
    CLIENT
}
