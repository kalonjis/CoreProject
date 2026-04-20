package be.steby.CoreProject.bll.domains.crm.today.services;

import be.steby.CoreProject.dal.repositories.crm.CommercialActionRepository;
import be.steby.CoreProject.dal.repositories.crm.DealRepository;
import be.steby.CoreProject.dal.repositories.crm.SupportTicketRepository;
import be.steby.CoreProject.dl.enums.crm.DealStatus;
import be.steby.CoreProject.dl.enums.crm.SupportTicketStatus;
import be.steby.CoreProject.pl.domains.commercialaction.models.responses.CommercialActionResponse;
import be.steby.CoreProject.pl.domains.deal.models.responses.DealSummaryResponse;
import be.steby.CoreProject.pl.domains.supportticket.models.responses.SupportTicketSummaryResponse;
import be.steby.CoreProject.pl.domains.today.models.responses.TodaySummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Default implementation of {@link TodayService}.
 *
 * <p>All queries are read-only and scoped to the server's default timezone.
 * Day boundaries are computed at runtime so the view always reflects the actual calendar day.</p>
 *
 * <ul>
 *   <li><b>Overdue actions</b> — commercial actions whose due date is before now and are not yet completed.</li>
 *   <li><b>Today's actions</b> — commercial actions due between start-of-day and end-of-day.</li>
 *   <li><b>Overdue deals</b> — open deals whose expected close date has passed.</li>
 *   <li><b>Deals closing soon</b> — open deals closing within the next 7 days.</li>
 *   <li><b>Open tickets</b> — support tickets in {@code OPEN} or {@code IN_PROGRESS} status.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodayServiceImpl implements TodayService {

    private final CommercialActionRepository commercialActionRepository;
    private final DealRepository             dealRepository;
    private final SupportTicketRepository    supportTicketRepository;

    /** {@inheritDoc} */
    @Override
    public TodaySummaryResponse getSummary() {

        ZoneId zone       = ZoneId.systemDefault();
        LocalDate today   = LocalDate.now(zone);
        Instant now       = Instant.now();
        Instant startOfDay = today.atStartOfDay(zone).toInstant();
        Instant endOfDay   = today.plusDays(1).atStartOfDay(zone).toInstant();

        List<CommercialActionResponse> overdueActions = commercialActionRepository
                .findAllOverdue(now)
                .stream().map(CommercialActionResponse::fromEntity).toList();

        List<CommercialActionResponse> todayActions = commercialActionRepository
                .findDueToday(startOfDay, endOfDay)
                .stream().map(CommercialActionResponse::fromEntity).toList();

        List<DealSummaryResponse> dealsOverdue = dealRepository
                .findByExpectedCloseDateBeforeAndStatus(today, DealStatus.OPEN)
                .stream().map(DealSummaryResponse::fromEntity).toList();

        List<DealSummaryResponse> dealsClosingSoon = dealRepository
                .findByExpectedCloseDateBetweenAndStatusOrderByExpectedCloseDateAsc(
                        today, today.plusDays(7), DealStatus.OPEN)
                .stream().map(DealSummaryResponse::fromEntity).toList();

        List<SupportTicketSummaryResponse> openTickets = supportTicketRepository
                .findByStatusInOrderByCreatedAtAsc(
                        List.of(SupportTicketStatus.OPEN, SupportTicketStatus.IN_PROGRESS))
                .stream().map(SupportTicketSummaryResponse::fromEntity).toList();

        return new TodaySummaryResponse(
                overdueActions,
                todayActions,
                dealsOverdue,
                dealsClosingSoon,
                openTickets
        );
    }
}
