package be.steby.CoreProject.il.telephony.twilio;

import be.steby.CoreProject.bll.domains.crm.call.models.TerminateCallRequest;
import be.steby.CoreProject.bll.domains.crm.call.services.CallService;
import be.steby.CoreProject.dal.repositories.crm.CallSessionRepository;
import be.steby.CoreProject.dl.entities.crm.CallSession;
import be.steby.CoreProject.dl.enums.crm.CallSessionStatus;
import be.steby.CoreProject.il.telephony.twilio.exceptions.InvalidTwilioSignatureException;
import com.twilio.security.RequestValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Handles incoming Twilio webhook events.
 *
 * <h3>Responsibilities</h3>
 * <ol>
 *   <li>Validates the {@code X-Twilio-Signature} header using the Auth Token
 *       from {@code TWILIO_AUTH_TOKEN} (env var / telephony.yml).</li>
 *   <li>Routes call status transitions to the appropriate {@link CallService} methods.</li>
 * </ol>
 *
 * <h3>Supported Twilio statuses</h3>
 * <ul>
 *   <li>{@code ringing} → session {@code RINGING}</li>
 *   <li>{@code in-progress} → session {@code ACTIVE} + {@code answeredAt} set</li>
 *   <li>{@code completed} → session {@code ENDED} + {@code durationSeconds} from Twilio</li>
 *   <li>{@code busy} / {@code no-answer} / {@code canceled} → session {@code MISSED}</li>
 *   <li>{@code failed} → session {@code FAILED}</li>
 * </ul>
 *
 * <p>All service calls are wrapped in try/catch to silently skip transitions that are
 * already in a terminal state (e.g. when the frontend terminates first and the webhook
 * arrives late).</p>
 *
 * <h3>URL configuration</h3>
 * <p>Set {@code TWILIO_WEBHOOK_BASE_URL} (e.g. {@code https://api.example.com}) so that
 * the request URL passed to {@link RequestValidator} matches what Twilio used.
 * Leave empty to use the URL reconstructed from the HTTP request (works when not behind a proxy).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TwilioWebhookHandler {

    private final CallSessionRepository callSessionRepository;
    private final CallService           callService;

    @Value("${telephony.twilio.base-url:}")
    private String baseUrl;

    @Value("${telephony.twilio.auth-token:}")
    private String authToken;

    // =========================================================================
    // Signature verification
    // =========================================================================

    /**
     * Verifies the {@code X-Twilio-Signature} header.
     *
     * @param requestUrl      full URL of the endpoint (as Twilio sees it)
     * @param twilioSignature value of the {@code X-Twilio-Signature} header
     * @param params          POST parameters from the request body
     * @throws InvalidTwilioSignatureException if the header is missing or the signature is invalid (HTTP 403)
     */
    public void verifySignature(String requestUrl, String twilioSignature, Map<String, String> params) {
        if (twilioSignature == null || twilioSignature.isBlank()) {
            throw new InvalidTwilioSignatureException("Missing X-Twilio-Signature header");
        }
        RequestValidator validator = new RequestValidator(authToken);
        if (!validator.validate(requestUrl, params, twilioSignature)) {
            log.warn("Twilio signature validation failed for URL: {}", requestUrl);
            throw new InvalidTwilioSignatureException("Invalid Twilio signature");
        }
    }

    /**
     * Builds the request URL as seen by Twilio.
     * Uses the configured {@code baseUrl} override when set (recommended in proxied deployments).
     *
     * @param scheme   e.g. {@code https}
     * @param host     e.g. {@code api.example.com}
     * @param path     e.g. {@code /api/crm/telephony/twilio/webhook}
     */
    public String buildRequestUrl(String scheme, String host, String path) {
        if (baseUrl != null && !baseUrl.isBlank()) {
            return baseUrl + path;
        }
        return scheme + "://" + host + path;
    }

    // =========================================================================
    // Status routing
    // =========================================================================

    /**
     * Processes a Twilio status-callback payload.
     * The session is looked up by {@code CallSid} (stored as {@code externalCallId}).
     * Unknown sessions or unknown statuses are silently ignored.
     *
     * @param params POST parameters from Twilio
     */
    public void handleStatusCallback(Map<String, String> params) {
        String callSid    = params.get("CallSid");
        String callStatus = params.get("CallStatus");

        if (callSid == null || callStatus == null) {
            log.debug("Twilio webhook missing CallSid or CallStatus — ignored");
            return;
        }

        Optional<CallSession> sessionOpt = callSessionRepository.findByExternalCallId(callSid);
        if (sessionOpt.isEmpty()) {
            log.debug("No CRM session found for CallSid: {} — ignored", callSid);
            return;
        }

        CallSession session = sessionOpt.get();
        log.debug("Twilio webhook — callSid: {}, status: {}, session: {}", callSid, callStatus, session.getPublicId());

        switch (callStatus) {
            case "ringing"     -> safeRing(session);
            case "in-progress" -> safeAnswer(session);
            case "completed"   -> safeTerminate(session, CallSessionStatus.ENDED,  parseDuration(params.get("CallDuration")));
            case "busy",
                 "no-answer",
                 "canceled"    -> safeTerminate(session, CallSessionStatus.MISSED, null);
            case "failed"      -> safeTerminate(session, CallSessionStatus.FAILED, null);
            default            -> log.debug("Unhandled Twilio status: {}", callStatus);
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private void safeRing(CallSession session) {
        try {
            callService.ring(session.getPublicId(), session.getPerformedBy());
        } catch (Exception e) {
            log.debug("ring() skipped for session {}: {}", session.getPublicId(), e.getMessage());
        }
    }

    private void safeAnswer(CallSession session) {
        try {
            callService.answer(session.getPublicId(), session.getPerformedBy());
        } catch (Exception e) {
            log.debug("answer() skipped for session {}: {}", session.getPublicId(), e.getMessage());
        }
    }

    private void safeTerminate(CallSession session, CallSessionStatus status, Integer durationSeconds) {
        try {
            callService.terminate(
                    session.getPublicId(),
                    new TerminateCallRequest(status, durationSeconds),
                    session.getPerformedBy()
            );
        } catch (Exception e) {
            log.debug("terminate() skipped for session {}: {}", session.getPublicId(), e.getMessage());
        }
    }

    private static Integer parseDuration(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
