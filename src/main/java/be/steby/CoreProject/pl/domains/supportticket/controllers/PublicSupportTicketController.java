package be.steby.CoreProject.pl.domains.supportticket.controllers;

import be.steby.CoreProject.bll.domains.supportticket.exceptions.SupportTicketRateLimitException;
import be.steby.CoreProject.bll.domains.supportticket.services.SupportTicketService;
import be.steby.CoreProject.dl.entities.crm.SupportTicket;
import be.steby.CoreProject.il.routes.supportticket.PublicSupportTicketRoutes;
import be.steby.CoreProject.pl.domains.supportticket.models.requests.PublicSupportTicketSubmitRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for public support ticket submissions.
 *
 * <p>Accessible without authentication. Handles submissions from the public
 * contact form when the visitor selects "Report a problem".</p>
 *
 * <p><strong>Endpoint:</strong> POST /api/public/support</p>
 *
 * <p>Security measures:</p>
 * <ul>
 *   <li>Honeypot field detection (silent fake-success for bots)</li>
 *   <li>IP-based rate limiting (5 submissions per 60 minutes by default)</li>
 * </ul>
 */
@RestController
@RequestMapping(PublicSupportTicketRoutes.BASE)
@RequiredArgsConstructor
@Slf4j
public class PublicSupportTicketController {

    private final SupportTicketService supportTicketService;

    /**
     * Submits a support ticket from the public contact form.
     *
     * @param request     the form data
     * @param httpRequest HTTP request for IP extraction
     * @return 201 Created with the ticket reference
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> submit(
            @Valid @RequestBody PublicSupportTicketSubmitRequest request,
            HttpServletRequest httpRequest) {

        log.info("Public support ticket submission — email: {}", request.email());

        SupportTicket ticket = supportTicketService.submitFromPublicForm(
                request.toBllModel(), httpRequest);

        log.info("Public support ticket accepted — publicId: {}", ticket.getPublicId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "reference", ticket.getPublicId() != null ? ticket.getPublicId() : "submitted",
                        "message", "Your request has been received. Our team will get back to you shortly."
                ));
    }

    // =========================================================================
    // Exception Handlers
    // =========================================================================

    @ExceptionHandler(SupportTicketRateLimitException.class)
    public ResponseEntity<Map<String, String>> handleRateLimit(SupportTicketRateLimitException e) {
        log.warn("Public support form rate limit exceeded: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("message", e.getMessage()));
    }
}
