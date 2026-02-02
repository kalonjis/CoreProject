package be.steby.CoreProject.bll.domains.lead.services;

import be.steby.CoreProject.bll.domains.lead.models.LeadRequest;
import be.steby.CoreProject.bll.domains.lead.models.LeadResult;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Service for handling public inquiry submissions.
 *
 * <p>Manages the complete inquiry flow for anonymous visitors including
 * validation, rate limiting, persistence, and email notification.</p>
 */
public interface LeadService {

    /**
     * Processes a public inquiry submission.
     *
     * @param request     the inquiry form data
     * @param httpRequest the HTTP request (for IP extraction)
     * @return the result of the submission
     */
    LeadResult submitInquiry(LeadRequest request, HttpServletRequest httpRequest);
}