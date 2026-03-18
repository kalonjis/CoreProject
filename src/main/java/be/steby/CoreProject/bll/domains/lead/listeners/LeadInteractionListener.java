package be.steby.CoreProject.bll.domains.lead.listeners;

import be.steby.CoreProject.bll.domains.lead.events.LeadSubmittedEvent;
import be.steby.CoreProject.dal.repositories.crm.InteractionRepository;
import be.steby.CoreProject.dl.entities.crm.Interaction;
import be.steby.CoreProject.dl.enums.crm.InteractionDirection;
import be.steby.CoreProject.dl.enums.crm.InteractionType;
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
 * <p>Persists a {@code CONTACT_FORM} interaction linked to the lead so that the
 * visitor's original message is visible in the lead timeline — and later in the
 * contact or deal timeline after conversion.</p>
 *
 * <p>{@code performedBy} is intentionally {@code null}: the author is the anonymous
 * visitor, not a CRM user.</p>
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

        Interaction interaction = Interaction.builder()
                .type(InteractionType.CONTACT_FORM)
                .direction(InteractionDirection.INBOUND)
                .subject(event.lead().getSubject())
                .notes(event.message())
                .occurredAt(event.lead().getSubmittedAt() != null
                        ? event.lead().getSubmittedAt()
                        : Instant.now())
                .lead(event.lead())
                .build();

        interactionRepository.save(interaction);

        log.info("CONTACT_FORM interaction created for lead {}", event.lead().getPublicId());
    }
}
