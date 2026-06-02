package be.steby.CoreProject.pl.domains.telephony.sip.controllers;

import be.steby.CoreProject.bll.domains.crm.telephony.sip.services.CommercialSipConfigService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.CommercialSipConfig;
import be.steby.CoreProject.il.telephony.crypto.TelephonyCredentialsEncryptionService;
import be.steby.CoreProject.pl.domains.telephony.sip.models.requests.SaveCommercialSipConfigHttpRequest;
import be.steby.CoreProject.pl.domains.telephony.sip.models.responses.CommercialSipConfigResponse;
import be.steby.CoreProject.pl.domains.telephony.sip.models.responses.SipConnectionDetailsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * REST controller for managing per-commercial SIP extension credentials.
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>GET  /api/crm/telephony/sip/connection — commercial: fetch own SIP credentials for SIP.js</li>
 *   <li>POST /api/crm/telephony/sip — admin: assign SIP config to a commercial</li>
 *   <li>GET  /api/crm/telephony/sip/{publicId} — admin: fetch one config</li>
 *   <li>PUT  /api/crm/telephony/sip/{publicId} — admin: update a config</li>
 *   <li>DELETE /api/crm/telephony/sip/{publicId} — admin: delete a config</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/crm/telephony/sip")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "CRM - SIP Config", description = "Per-commercial SIP extension credentials for SIP.js")
public class CommercialSipConfigController {

    private final CommercialSipConfigService            sipConfigService;
    private final TelephonyCredentialsEncryptionService encryptionService;

    @Value("${telephony.sip.ws-url}")
    private String sipWsUrl;

    @Value("${telephony.sip.domain}")
    private String sipDomain;

    /**
     * Returns the SIP connection details for the authenticated commercial.
     *
     * <p>Called once when the commercial loads the CRM, so SIP.js can register
     * against Asterisk before the first call. Transmitted over HTTPS only.</p>
     */
    @GetMapping("/connection")
    @PreAuthorize("hasAnyAuthority('COMMERCIAL', 'ADMIN')")
    @Operation(summary = "Get own SIP connection details", description = "Returns Asterisk WebSocket URL and personal SIP credentials for SIP.js initialisation")
    public ResponseEntity<SipConnectionDetailsResponse> getConnectionDetails(
            @AuthenticationPrincipal User actor) {

        CommercialSipConfig sipConfig = sipConfigService.getForActor(actor);
        String password = encryptionService.decrypt(sipConfig.getEncryptedSipPassword());

        return ResponseEntity.ok(new SipConnectionDetailsResponse(
                sipWsUrl,
                sipDomain,
                sipConfig.getSipUsername(),
                password,
                sipConfig.getDisplayName()
        ));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Create SIP config for a commercial")
    public ResponseEntity<CommercialSipConfigResponse> create(
            @Valid @RequestBody SaveCommercialSipConfigHttpRequest request) {

        log.info("CommercialSipConfig creation — sipUsername: {}", request.sipUsername());
        CommercialSipConfig config = sipConfigService.create(request.toBllModel());
        URI location = URI.create("/api/crm/telephony/sip/" + config.getPublicId());
        return ResponseEntity.created(location).body(CommercialSipConfigResponse.fromEntity(config));
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Get a SIP config by public ID")
    public ResponseEntity<CommercialSipConfigResponse> getByPublicId(@PathVariable String publicId) {
        return ResponseEntity.ok(
                CommercialSipConfigResponse.fromEntity(sipConfigService.getByPublicId(publicId)));
    }

    @PutMapping("/{publicId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Update a commercial's SIP config")
    public ResponseEntity<CommercialSipConfigResponse> update(
            @PathVariable String publicId,
            @Valid @RequestBody SaveCommercialSipConfigHttpRequest request) {

        log.info("CommercialSipConfig update — publicId: {}", publicId);
        return ResponseEntity.ok(
                CommercialSipConfigResponse.fromEntity(
                        sipConfigService.update(publicId, request.toBllModel())));
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Delete a commercial's SIP config")
    public ResponseEntity<Void> delete(@PathVariable String publicId) {
        log.info("CommercialSipConfig deletion — publicId: {}", publicId);
        sipConfigService.delete(publicId);
        return ResponseEntity.noContent().build();
    }

}
