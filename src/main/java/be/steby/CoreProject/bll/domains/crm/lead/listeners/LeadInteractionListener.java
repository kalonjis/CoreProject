package be.steby.CoreProject.bll.domains.crm.lead.listeners;

import be.steby.CoreProject.bll.domains.crm.lead.events.LeadSubmittedEvent;
import be.steby.CoreProject.dal.repositories.crm.InteractionRepository;
import be.steby.CoreProject.dl.entities.crm.Interaction;
import be.steby.CoreProject.dl.enums.crm.InteractionDirection;
import be.steby.CoreProject.dl.enums.crm.InteractionType;
import be.steby.CoreProject.dl.enums.crm.LeadSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Listener that auto-creates the first CRM interaction when a lead is submitted.
 *
 * <p>For web-form leads ({@code LeadSource != MANUAL}): persists a {@code CONTACT_FORM}
 * interaction (direction INBOUND) so the visitor's original message is visible in the
 * lead timeline.</p>
 *
 * <p>For manually-created leads ({@code LeadSource.MANUAL}): persists a {@code NOTE}
 * interaction (no direction) if the actor provided an initial note.</p>
 *
 * <p>{@code performedBy} is intentionally {@code null} in both cases.</p>
 */
@Component
@Order(5)
@RequiredArgsConstructor
@Slf4j
public class LeadInteractionListener {

    private final InteractionRepository interactionRepository;

    @EventListener
    @Transactional
    public void handleLeadSubmitted(LeadSubmittedEvent event) {
        if (event.message() == null || event.message().isBlank()) return;

        boolean isManual = LeadSource.MANUAL == event.lead().getLeadSource();

        Interaction.InteractionBuilder builder = Interaction.builder()
                .type(isManual ? InteractionType.NOTE : InteractionType.CONTACT_FORM)
                .subject(event.lead().getSubject())
                .notes(event.message())
                .occurredAt(event.lead().getSubmittedAt() != null
                        ? event.lead().getSubmittedAt()
                        : Instant.now())
                .lead(event.lead());

        if (!isManual) {
            builder.direction(InteractionDirection.INBOUND);
        }

        interactionRepository.save(builder.build());

        log.info("{} interaction created for lead {}",
                isManual ? "NOTE" : "CONTACT_FORM", event.lead().getPublicId());
    }
}
