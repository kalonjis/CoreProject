package be.steby.CoreProject.bll.domains.contact.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Contact;
import be.steby.CoreProject.dl.entities.crm.Lead;

import java.time.Instant;

/**
 * Domain event published when a {@link Contact} is created as a result
 * of a {@link Lead} conversion.
 *
 * <p>Distinct from {@link ContactCreatedEvent} — listeners may react
 * differently here, for example:</p>
 * <ul>
 *   <li>Notify the commercial who was assigned to the originating lead</li>
 *   <li>Log the conversion in the activity log with lead traceability</li>
 *   <li>Trigger a welcome sequence specific to converted leads</li>
 * </ul>
 *
 * <h3>Correlation</h3>
 * <p>The {@code originLead} carried here is the same lead whose
 * {@code LeadConvertedEvent} triggered this contact creation.
 * Both events share the same {@code contactPublicId} as a correlation handle.</p>
 *
 * @param contact     the newly created contact
 * @param originLead  the lead that was converted into this contact
 * @param actor       the user who performed the conversion
 * @param actorDevice the device from which the action was initiated
 * @param timestamp   when the event occurred
 */
public record ContactCreatedFromLeadEvent(
        Contact contact,
        Lead originLead,
        User actor,
        Device actorDevice,
        Instant timestamp
) {
    public ContactCreatedFromLeadEvent(Contact contact, Lead originLead, User actor, Device actorDevice) {
        this(contact, originLead, actor, actorDevice, Instant.now());
    }
}