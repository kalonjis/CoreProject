package be.steby.CoreProject.bll.domains.lead.events;

import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Lead;

/**
 * Event published when a lead is successfully converted into a Contact.
 *
 * <p>This is the terminal success event of the lead lifecycle.
 * It is the primary cross-domain integration point between the
 * {@code lead} domain and the {@code contact} domain.</p>
 *
 * <p>Consumed by:</p>
 * <ul>
 *   <li>{@code ContactCreationListener} (contact domain) — creates the Contact from lead data</li>
 *   <li>Notification listener — notifies the converting commercial</li>
 * </ul>
 *
 * <p>The {@code contactPublicId} is set by the service layer once the
 * Contact has been persisted, providing a correlation handle for listeners
 * that need to reference the newly created Contact without a direct
 * domain coupling.</p>
 *
 * @param lead            the lead that was converted
 * @param convertedBy     the commercial who performed the conversion
 * @param contactPublicId the public UUID of the Contact created from this lead
 * @param actorDevice     the device from which the conversion was performed
 */
public record LeadConvertedEvent(
        Lead lead,
        User convertedBy,
        String contactPublicId,
        Device actorDevice
) {}