package be.steby.CoreProject.bll.domains.crm.deal.listeners;

import be.steby.CoreProject.bll.domains.crm.deal.services.DealServiceImpl;
import be.steby.CoreProject.bll.domains.crm.deal.events.DealCreatedEvent;
import be.steby.CoreProject.bll.domains.crm.deal.events.DealStageChangedEvent;
import be.steby.CoreProject.dal.repositories.crm.DealStageHistoryRepository;
import be.steby.CoreProject.dl.entities.crm.DealStageHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Records stage history entries whenever a deal is created or moves stage.
 *
 * <p>Keeps {@link DealServiceImpl}
 * free of history-recording concerns. Each entry captures when a deal entered a
 * {@link be.steby.CoreProject.dl.entities.crm.PipelineStep} and, once the deal
 * moves on, when it left.</p>
 *
 * <h3>Entry lifecycle</h3>
 * <ul>
 *   <li>{@link DealCreatedEvent} → insert initial entry ({@code exitedAt = null})</li>
 *   <li>{@link DealStageChangedEvent} → close previous open entry, insert new entry</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DealStageHistoryListener {

    private final DealStageHistoryRepository dealStageHistoryRepository;

    @EventListener
    @Transactional
    public void onDealCreated(DealCreatedEvent event) {
        dealStageHistoryRepository.save(DealStageHistory.builder()
                .deal(event.deal())
                .pipelineStep(event.deal().getPipelineStep())
                .enteredAt(event.deal().getCreatedAt())
                .build());

        log.debug("DealStageHistoryListener — initial entry for deal {} at stage {}",
                event.deal().getPublicId(), event.deal().getPipelineStep().getName());
    }

    @EventListener
    @Transactional
    public void onDealStageChanged(DealStageChangedEvent event) {
        Instant now = event.timestamp();

        dealStageHistoryRepository.findOpenEntryByDealId(event.deal().getId()).ifPresent(entry -> {
            entry.setExitedAt(now);
            dealStageHistoryRepository.save(entry);
        });

        dealStageHistoryRepository.save(DealStageHistory.builder()
                .deal(event.deal())
                .pipelineStep(event.newStep())
                .enteredAt(now)
                .build());

        log.debug("DealStageHistoryListener — deal {} moved {} → {}",
                event.deal().getPublicId(), event.previousStep().getName(), event.newStep().getName());
    }
}
