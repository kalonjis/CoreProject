package be.steby.CoreProject.bll.domains.dashboard.services;

import be.steby.CoreProject.dal.repositories.crm.CommercialActionRepository;
import be.steby.CoreProject.dal.repositories.crm.DealRepository;
import be.steby.CoreProject.dal.repositories.crm.LeadRepository;
import be.steby.CoreProject.dal.repositories.crm.SupportTicketRepository;
import be.steby.CoreProject.dl.enums.crm.DealStatus;
import be.steby.CoreProject.dl.enums.crm.LeadStatus;
import be.steby.CoreProject.dl.enums.crm.SupportTicketStatus;
import be.steby.CoreProject.pl.domains.dashboard.models.responses.CrmStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;

/**
 * Implementation of {@link CrmDashboardService}.
 *
 * <p>All queries run in a single read-only transaction.
 * Monthly figures are computed from the first day of the current month at midnight (system zone).</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrmDashboardServiceImpl implements CrmDashboardService {

    private final LeadRepository               leadRepository;
    private final DealRepository               dealRepository;
    private final CommercialActionRepository   commercialActionRepository;
    private final SupportTicketRepository      supportTicketRepository;

    @Override
    public CrmStatsResponse getStats() {
        Instant startOfMonth = YearMonth.now()
                .atDay(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();

        Instant now = Instant.now();

        BigDecimal pipelineValue  = dealRepository.sumAmountByStatus(DealStatus.OPEN);
        BigDecimal revenueWon     = dealRepository.sumAmountWonSince(startOfMonth);
        BigDecimal forecastRevenue = dealRepository.sumWeightedForecast();

        return new CrmStatsResponse(
                leadRepository.countByStatus(LeadStatus.NEW),
                leadRepository.countByStatus(LeadStatus.IN_REVIEW),
                dealRepository.countByStatus(DealStatus.OPEN),
                pipelineValue   != null ? pipelineValue   : BigDecimal.ZERO,
                dealRepository.countWonSince(startOfMonth),
                revenueWon      != null ? revenueWon      : BigDecimal.ZERO,
                commercialActionRepository.countAllOverdue(now),
                supportTicketRepository.countByStatus(SupportTicketStatus.OPEN),
                supportTicketRepository.countByStatus(SupportTicketStatus.IN_PROGRESS),
                forecastRevenue != null ? forecastRevenue : BigDecimal.ZERO
        );
    }
}
