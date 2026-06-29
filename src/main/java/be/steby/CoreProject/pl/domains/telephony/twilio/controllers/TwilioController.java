package be.steby.CoreProject.pl.domains.telephony.twilio.controllers;

import be.steby.CoreProject.bll.domains.crm.call.services.CallService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.il.telephony.twilio.TwilioTokenService;
import be.steby.CoreProject.il.telephony.twilio.TwilioWebhookHandler;
import be.steby.CoreProject.pl.domains.telephony.twilio.models.responses.TwilioTokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for Twilio Voice SDK integration.
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code GET  /api/crm/telephony/twilio/token}   — Access Token for the frontend Twilio Device (authenticated)</li>
 *   <li>{@code POST /api/crm/telephony/twilio/twiml}   — TwiML instructions for Twilio (public, Twilio-signed)</li>
 *   <li>{@code POST /api/crm/telephony/twilio/webhook} — Status callbacks from Twilio (public, Twilio-signed)</li>
 * </ul>
 *
 * <h3>Twilio App setup</h3>
 * <p>In the Twilio Console, create a TwiML Application and set:</p>
 * <ul>
 *   <li>Voice Request URL: {@code POST {TWILIO_WEBHOOK_BASE_URL}/api/crm/telephony/twilio/twiml}</li>
 *   <li>Status Callback URL: {@code POST {TWILIO_WEBHOOK_BASE_URL}/api/crm/telephony/twilio/webhook}</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/crm/telephony/twilio")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "CRM - Telephony (Twilio)", description = "Twilio Voice SDK token issuance and webhook handling")
public class TwilioController {

    private final TwilioTokenService   tokenService;
    private final TwilioWebhookHandler webhookHandler;
    private final CallService          callService;

    @Value("${telephony.twilio.caller-id:}")
    private String callerId;

    // =========================================================================
    // Token (authenticated)
    // =========================================================================

    /**
     * Issues a Twilio Access Token for the frontend Twilio Voice SDK.
     *
     * <p><strong>Security:</strong> requires {@code COMMERCIAL} or {@code ADMIN} authority.</p>
     */
    @GetMapping("/token")
    @PreAuthorize("hasAnyAuthority('COMMERCIAL', 'ADMIN')")
    @Operation(summary = "Get Twilio Access Token", description = "Returns a short-lived JWT for the Twilio Voice SDK Device")
    public ResponseEntity<TwilioTokenResponse> getToken(@AuthenticationPrincipal User actor) {
        log.debug("Twilio token requested by user: {}", actor.getId());
        String jwt = tokenService.generateAccessToken(actor);
        return ResponseEntity.ok(new TwilioTokenResponse(jwt));
    }

    // =========================================================================
    // TwiML (public — verified by Twilio signature)
    // =========================================================================

    /**
     * Returns TwiML instructions for an outbound call initiated via the Twilio Voice SDK.
     *
     * <p>Twilio calls this endpoint when {@code device.connect()} is invoked in the browser.
     * The {@code callPublicId} custom parameter (set by the frontend) is used to register
     * the Twilio {@code CallSid} on the CRM session for later webhook correlation.</p>
     *
     * <p><strong>Security:</strong> public endpoint, protected by Twilio signature verification.</p>
     */
    @PostMapping(value = "/twiml", produces = MediaType.TEXT_XML_VALUE)
    @Operation(summary = "TwiML App handler", description = "Returns TwiML for an outbound call (called by Twilio, not by the frontend)")
    public ResponseEntity<String> twiml(
            @RequestParam Map<String, String> params,
            HttpServletRequest request) {

        String callSid      = params.get("CallSid");
        String callPublicId = params.get("callPublicId");
        String to           = params.get("To");

        log.debug("Twilio TwiML request — callSid: {}, callPublicId: {}, to: {}", callSid, callPublicId, to);

        verifySignature(request, params);

        if (callPublicId != null && !callPublicId.isBlank() && callSid != null) {
            try {
                callService.registerExternalCallId(callPublicId, callSid);
            } catch (Exception e) {
                log.warn("Could not register externalCallId for session {}: {}", callPublicId, e.getMessage());
            }
        }

        String twiml = buildDialTwiml(callerId, to);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_XML)
                .body(twiml);
    }

    // =========================================================================
    // Webhook (public — verified by Twilio signature)
    // =========================================================================

    /**
     * Receives Twilio status-callback events for the parent call.
     *
     * <p>Routes lifecycle events ({@code ringing}, {@code in-progress}, {@code completed}, …)
     * to the {@link CallService} to keep the CRM session state in sync.</p>
     *
     * <p><strong>Security:</strong> public endpoint, protected by Twilio signature verification.</p>
     */
    @PostMapping("/webhook")
    @Operation(summary = "Twilio status webhook", description = "Receives call status updates from Twilio (ringing, in-progress, completed, etc.)")
    public ResponseEntity<Void> webhook(
            @RequestParam Map<String, String> params,
            HttpServletRequest request) {

        log.debug("Twilio webhook — status: {}, callSid: {}", params.get("CallStatus"), params.get("CallSid"));

        verifySignature(request, params);
        webhookHandler.handleStatusCallback(params);

        return ResponseEntity.ok().build();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private void verifySignature(HttpServletRequest request, Map<String, String> params) {
        String signature = request.getHeader("X-Twilio-Signature");
        String url = webhookHandler.buildRequestUrl(
                request.getScheme(),
                request.getHeader("host") != null ? request.getHeader("host") : request.getServerName(),
                request.getRequestURI()
        );
        webhookHandler.verifySignature(url, signature, params);
    }

    private String buildDialTwiml(String fromCallerId, String to) {
        if (to != null && to.startsWith("client:")) {
            String identity = escapeXml(to.substring("client:".length()));
            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                   "<Response>" +
                   "<Dial><Client>" + identity + "</Client></Dial>" +
                   "</Response>";
        }
        String callerIdAttr = (fromCallerId != null && !fromCallerId.isBlank())
                ? " callerId=\"" + escapeXml(fromCallerId) + "\""
                : "";
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
               "<Response>" +
               "<Dial" + callerIdAttr + ">" +
               "<Number>" + escapeXml(to != null ? to : "") + "</Number>" +
               "</Dial>" +
               "</Response>";
    }

    private static String escapeXml(String value) {
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");
    }
}
