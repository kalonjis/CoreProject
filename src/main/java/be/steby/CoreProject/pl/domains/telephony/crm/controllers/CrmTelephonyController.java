package be.steby.CoreProject.pl.domains.telephony.crm.controllers;

import be.steby.CoreProject.bll.domains.crm.telephony.sip.exceptions.SipConfigNotFoundException;
import be.steby.CoreProject.bll.domains.crm.telephony.sip.services.CommercialSipConfigService;
import be.steby.CoreProject.dl.entities.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lightweight controller exposing per-user telephony metadata for the CRM frontend.
 *
 * <p>The frontend calls {@code GET /api/crm/telephony/my-provider} once on CRM shell load
 * to know which SDK to initialize (SIP.js, Twilio, or none). Only one provider is ever
 * active for a given deployment — no mixing.</p>
 */
@RestController
@RequestMapping("/api/crm/telephony")
@PreAuthorize("hasAnyAuthority('COMMERCIAL', 'ADMIN')")
@RequiredArgsConstructor
@Tag(name = "CRM - Telephony", description = "Per-user telephony metadata")
public class CrmTelephonyController {

    private final CommercialSipConfigService sipConfigService;

    @Value("${telephony.global-provider:TEL_URI}")
    private String globalProvider;

    /**
     * Returns the effective telephony provider for the authenticated user.
     *
     * <p>Priority:
     * <ol>
     *   <li>User has a per-user SIP config → {@code SIP}</li>
     *   <li>Global provider from {@code telephony.global-provider} (env: {@code TELEPHONY_GLOBAL_PROVIDER})</li>
     * </ol>
     */
    @GetMapping("/my-provider")
    @Operation(summary = "Get effective telephony provider for the current user")
    public ResponseEntity<CrmTelephonyProviderResponse> getMyProvider(
            @AuthenticationPrincipal User actor) {

        try {
            sipConfigService.getForActor(actor);
            return ResponseEntity.ok(new CrmTelephonyProviderResponse("SIP"));
        } catch (SipConfigNotFoundException ignored) {}

        return ResponseEntity.ok(new CrmTelephonyProviderResponse(globalProvider));
    }

    public record CrmTelephonyProviderResponse(String provider) {}
}
