package be.steby.CoreProject.pl.domains.call.controllers;

import be.steby.CoreProject.bll.domains.crm.call.services.CallService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.CallSession;
import be.steby.CoreProject.pl.domains.call.models.requests.InitiateCallHttpRequest;
import be.steby.CoreProject.pl.domains.call.models.requests.TerminateCallHttpRequest;
import be.steby.CoreProject.pl.domains.call.models.responses.CallerInfoResponse;
import be.steby.CoreProject.pl.domains.call.models.responses.CallSessionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * REST controller for CRM telephony — call session lifecycle.
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/calls</pre>
 *
 * <h3>Security</h3>
 * <p>Requires {@code COMMERCIAL} or {@code ADMIN} authority on all endpoints.</p>
 *
 * <h3>Call flow</h3>
 * <ol>
 *   <li>{@code POST /api/crm/calls} — initiate; frontend triggers the {@code tel:} URI or in-app dialler</li>
 *   <li>{@code PATCH /api/crm/calls/{publicId}/terminate} — user confirms outcome; interaction is logged automatically</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/crm/calls")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('COMMERCIAL', 'ADMIN')")
@Slf4j
@Tag(name = "CRM - Calls", description = "Call session lifecycle management")
public class CrmCallController {

    private final CallService callService;

    /**
     * Resolves the display name and CRM identity of an inbound caller.
     *
     * <p>Checks SIP extensions first (internal users), then CRM contacts by phone number.
     * Returns {@code displayName: null} when the caller is unknown.</p>
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/calls/caller-info?number=</p>
     */
    @GetMapping("/caller-info")
    @Operation(summary = "Resolve caller info", description = "Matches an inbound number against SIP extensions and CRM contacts")
    public ResponseEntity<CallerInfoResponse> getCallerInfo(@RequestParam String number) {
        return ResponseEntity.ok(CallerInfoResponse.from(callService.resolveCallerInfo(number)));
    }

    /**
     * Returns the detail of a single call session.
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/calls/{publicId}</p>
     */
    @GetMapping("/{publicId}")
    @Operation(summary = "Get call session", description = "Returns the detail of a call session by its public UUID")
    public ResponseEntity<CallSessionResponse> getByPublicId(@PathVariable String publicId) {
        log.debug("Call session detail requested — publicId: {}", publicId);

        CallSession session = callService.getByPublicId(publicId);
        return ResponseEntity.ok(CallSessionResponse.fromEntity(session));
    }

    /**
     * Initiates a call and creates the corresponding {@link CallSession}.
     *
     * <p>Returns immediately after persisting the session. The actual call is
     * placed by the telephony adapter (OS dialler, Twilio, or SIP) independently.</p>
     *
     * <p><strong>Endpoint:</strong> POST /api/crm/calls</p>
     */
    @PostMapping
    @Operation(summary = "Initiate call", description = "Creates a call session and triggers the configured telephony adapter")
    public ResponseEntity<CallSessionResponse> initiate(
            @Valid @RequestBody InitiateCallHttpRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Call initiation requested — number: {}, by: {}", request.phoneNumber(), actor.getId());

        CallSession session = callService.initiate(request.toBllModel(), actor);
        URI location = URI.create("/api/crm/calls/" + session.getPublicId());
        return ResponseEntity.created(location).body(CallSessionResponse.fromEntity(session));
    }

    /**
     * Records the moment the remote party answered the call.
     *
     * <p>Called by the frontend when SIP.js receives a {@code 200 OK}.
     * Sets {@code answeredAt} to the current server time and transitions status to {@code ACTIVE}.</p>
     *
     * <p><strong>Endpoint:</strong> PATCH /api/crm/calls/{publicId}/answer</p>
     */
    @PatchMapping("/{publicId}/answer")
    @Operation(summary = "Mark call as answered", description = "Records the answer timestamp when the remote party picks up (SIP 200 OK event)")
    public ResponseEntity<CallSessionResponse> answer(
            @PathVariable String publicId,
            @AuthenticationPrincipal User actor) {

        log.info("Call answered — publicId: {}, by: {}", publicId, actor.getId());

        CallSession session = callService.answer(publicId, actor);
        return ResponseEntity.ok(CallSessionResponse.fromEntity(session));
    }

    /**
     * Terminates an active call session and triggers interaction logging.
     *
     * <p>The {@code CallTerminatedInteractionListener} automatically creates the
     * {@code Interaction} and {@code CallLog} in the CRM timeline after this call.</p>
     *
     * <p><strong>Endpoint:</strong> PATCH /api/crm/calls/{publicId}/terminate</p>
     */
    @PatchMapping("/{publicId}/terminate")
    @Operation(summary = "Terminate call", description = "Terminates a call session and logs the interaction in the CRM timeline")
    public ResponseEntity<CallSessionResponse> terminate(
            @PathVariable String publicId,
            @Valid @RequestBody TerminateCallHttpRequest request,
            @AuthenticationPrincipal User actor) {

        log.info("Call termination requested — publicId: {}, status: {}, by: {}",
                publicId, request.status(), actor.getId());

        callService.terminate(publicId, request.toBllModel(), actor);
        CallSession session = callService.getByPublicId(publicId);
        return ResponseEntity.ok(CallSessionResponse.fromEntity(session));
    }
}
