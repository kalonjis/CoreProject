package be.steby.CoreProject.bll.domains.today.services;

import be.steby.CoreProject.pl.domains.today.models.responses.TodaySummaryResponse;

public interface TodayService {
    TodaySummaryResponse getSummary();
}
