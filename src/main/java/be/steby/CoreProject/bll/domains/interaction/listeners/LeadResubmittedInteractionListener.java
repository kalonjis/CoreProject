package be.steby.CoreProject.bll.domains.interaction.listeners;

import be.steby.CoreProject.bll.domains.lead.events.LeadResubmittedEvent;
import be.steby.CoreProject.dal.repositories.crm.InteractionRepository;
import be.steby.CoreProject.dl.entities.crm.Interaction;
import be.steby.CoreProject.dl.enums.crm.InteractionDirection;
import be.steby.CoreProject.dl.enums.crm.InteractionOutcome;
import be.steby.CoreProject.dl.enums.crm.InteractionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Logs a system NOTE interaction when a public form submission is a duplicate of
 * an existing active lead.
 *
 * <h3>Why a direct repository write instead of InteractionService</h3>
 * <p>{@link be.steby.CoreProject.bll.domains.interaction.services.InteractionService#create}
 * requires a non-null {@code User} actor. Public form submissions have no actor —
 * they originate from an anonymous visitor. Writing directly to the repository
 * allows {@code performedBy = null}, which the {@link Interaction} entity supports
 * for system-generated entries.</p>
 *
 * <h3>Domain ownership</h3>
 * <p>The {@code lead} domain publishes the event. This listener lives in the
 * {@code interaction} domain and owns the decision of how to record it in the
 * timeline — preserving SoC.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LeadResubmittedInteractionListener {

    private final InteractionRepository interactionRepository;

    @EventListener
    @Async("activityLogExecutor")
    @Transactional
    public void onLeadResubmitted(LeadResubmittedEvent event) {
        String notes = buildNotes(event.newSubject(), event.newMessage());

        Interaction interaction = Interaction.builder()
                .type(InteractionType.CONTACT_FORM)
                .direction(InteractionDirection.INBOUND)
                .subject("Nouvelle soumission via formulaire")
                .notes(notes)
                .outcome(InteractionOutcome.NEUTRAL)
                .occurredAt(Instant.now())
                .lead(event.existingLead())
                .performedBy(null)
                .build();

        interaction.setPublicId(UUID.randomUUID().toString());
        interactionRepository.save(interaction);

        log.info("LeadResubmittedInteractionListener — NOTE logged on lead {} (resubmission)",
                event.existingLead().getPublicId());
    }

    private String buildNotes(String subject, String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("Sujet : ").append(subject);
        if (message != null && !message.isBlank()) {
            sb.append("\n\n").append(message);
        }
        return sb.toString();
    }
}
