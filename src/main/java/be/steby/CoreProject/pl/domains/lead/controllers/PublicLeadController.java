package be.steby.CoreProject.pl.domains.lead.controllers;

import be.steby.CoreProject.bll.domains.crm.lead.exceptions.HoneypotDetectedException;
import be.steby.CoreProject.bll.domains.crm.lead.exceptions.LeadRateLimitException;
import be.steby.CoreProject.bll.domains.crm.lead.exceptions.LeadValidationException;
import be.steby.CoreProject.bll.domains.crm.lead.models.LeadResult;
import be.steby.CoreProject.bll.domains.crm.lead.services.LeadService;
import be.steby.CoreProject.pl.domains.lead.models.requests.SubmitLeadRequest;
import be.steby.CoreProject.pl.domains.lead.models.responses.LeadOperationResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for public inquiry operations.
 *
 * <p>Handles inquiry submissions from anonymous visitors through the
 * public contact form. This endpoint is accessible without authentication.</p>
 *
 * <p>Security measures:</p>
 * <ul>
 *   <li>Honeypot field detection (silent rejection)</li>
 *   <li>Rate limiting per IP and email</li>
 *   <li>Email format and domain validation</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/lead")
@RequiredArgsConstructor
@Slf4j
public class PublicLeadController {

    private final LeadService leadService;

    /**
     * Submits a public inquiry.
     *
     * <p>Accessible to anonymous visitors. Email is required.</p>
     *
     * <p><strong>Endpoint:</strong> POST /api/inquiry</p>
     *
     * @param request     the inquiry form data
     * @param httpRequest HTTP request for IP extraction
     * @return response with inquiry reference ID
     */
    @PostMapping
    public ResponseEntity<LeadOperationResponse> submitInquiry(
            @Valid @RequestBody SubmitLeadRequest request,
            HttpServletRequest httpRequest) {

        log.info("Public inquiry received - type: {}", request.leadType());

        try {
            LeadResult result = leadService.submitInquiry(
                    request.toBllModel(),
                    httpRequest
            );

            log.info("Public inquiry submitted successfully - referenceId: {}", result.publicId());

            return ResponseEntity.ok(LeadOperationResponse.from(result));

        } catch (HoneypotDetectedException e) {
            // Silent rejection - return fake success to not inform bots
            log.warn("Honeypot detected, returning fake success");
            return ResponseEntity.ok(LeadOperationResponse.honeypotFakeSuccess());
        }
    }

    // =========================================================================
    // Exception Handlers
    // =========================================================================

    /**
     * Handles rate limit exceeded exceptions.
     */
    @ExceptionHandler(LeadRateLimitException.class)
    public ResponseEntity<LeadOperationResponse> handleRateLimitExceeded(LeadRateLimitException e) {
        log.warn("Inquiry rate limit exceeded: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(LeadOperationResponse.error(e.getMessage()));
    }

    /**
     * Handles inquiry validation exceptions.
     */
    @ExceptionHandler(LeadValidationException.class)
    public ResponseEntity<LeadOperationResponse> handleValidationError(LeadValidationException e) {
        log.warn("Inquiry validation failed: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(LeadOperationResponse.error(e.getMessage()));
    }
}