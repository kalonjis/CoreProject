package be.steby.CoreProject.bll.domains.crm.interaction.listeners;

import be.steby.CoreProject.bll.domains.crm.interaction.models.InteractionCreateRequest;
import be.steby.CoreProject.bll.domains.crm.interaction.services.InteractionService;
import be.steby.CoreProject.bll.domains.crm.outreach.events.OutreachEmailSentEvent;
import be.steby.CoreProject.dl.enums.crm.InteractionDirection;
import be.steby.CoreProject.dl.enums.crm.InteractionOutcome;
import be.steby.CoreProject.dl.enums.crm.InteractionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listens to {@link OutreachEmailSentEvent} and delegates interaction creation
 * to {@link InteractionService}.
 *
 * <h3>Domain ownership</h3>
 * <p>Creating interactions is the responsibility of the {@code interaction} domain.
 * The {@code outreach} domain only publishes the event — it has no knowledge of
 * interaction storage or sub-entities ({@code EmailLog}).</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutreachEmailInteractionListener {

    private static final int SNIPPET_MAX_LENGTH = 200;

    private final InteractionService interactionService;

    @EventListener
    @Transactional
    public void handleOutreachEmailSent(OutreachEmailSentEvent event) {
        log.debug("Outreach event received — logging EMAIL interaction for contact: {}",
                event.contact().getPublicId());

        InteractionCreateRequest request = new InteractionCreateRequest(
                InteractionType.EMAIL,
                InteractionDirection.OUTBOUND,
                event.subject(),
                event.body(),
                InteractionOutcome.NEUTRAL,
                null,
                event.timestamp(),
                event.dealPublicId(),
                event.contact().getPublicId(),
                null,
                null,
                new InteractionCreateRequest.EmailLogDetails(
                        event.subject(),
                        buildSnippet(event.body()),
                        null
                )
        );

        interactionService.create(request, event.commercial());

        log.info("EMAIL interaction logged via outreach event — contact: {}, deal: {}",
                event.contact().getPublicId(), event.dealPublicId());
    }

    private String buildSnippet(String body) {
        if (body == null) return null;
        return body.length() <= SNIPPET_MAX_LENGTH
                ? body
                : body.substring(0, SNIPPET_MAX_LENGTH - 3) + "...";
    }
}
