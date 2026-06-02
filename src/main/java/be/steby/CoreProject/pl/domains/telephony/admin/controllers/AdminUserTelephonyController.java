package be.steby.CoreProject.pl.domains.telephony.admin.controllers;

import be.steby.CoreProject.bll.domains.crm.telephony.sip.exceptions.SipConfigNotFoundException;
import be.steby.CoreProject.bll.domains.crm.telephony.sip.models.SaveSipConfigRequest;
import be.steby.CoreProject.bll.domains.crm.telephony.sip.services.CommercialSipConfigService;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.CommercialSipConfig;
import be.steby.CoreProject.dl.enums.crm.CallProvider;
import be.steby.CoreProject.pl.domains.telephony.admin.models.requests.AdminSaveSipConfigHttpRequest;
import be.steby.CoreProject.pl.domains.telephony.admin.models.responses.AdminUserTelephonyStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin REST controller for managing a user's telephony configuration.
 *
 * <h3>Base path</h3>
 * <pre>/api/admin/telephony/users/{userPublicId}</pre>
 *
 * <h3>Effective provider logic</h3>
 * <ul>
 *   <li>SIP config assigned → effective = SIP (overrides global)</li>
 *   <li>No SIP + global = TWILIO → effective = TWILIO (identity = userPublicId)</li>
 *   <li>No SIP + global = other → effective = NONE</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/telephony/users")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - User Telephony", description = "Admin management of per-user telephony configuration")
public class AdminUserTelephonyController {

    private final UserService                userService;
    private final CommercialSipConfigService sipConfigService;

    @Value("${telephony.global-provider:TEL_URI}")
    private String globalProvider;

    // =========================================================================
    // GET status
    // =========================================================================

    @GetMapping("/{userPublicId}")
    @Operation(summary = "Get telephony status for a user")
    public ResponseEntity<AdminUserTelephonyStatusResponse> getStatus(
            @PathVariable String userPublicId) {

        return ResponseEntity.ok(buildStatus(userPublicId));
    }

    // =========================================================================
    // SIP — create
    // =========================================================================

    @PostMapping("/{userPublicId}/sip")
    @Operation(summary = "Assign SIP credentials to a user")
    public ResponseEntity<AdminUserTelephonyStatusResponse> createSipConfig(
            @PathVariable String userPublicId,
            @Valid @RequestBody AdminSaveSipConfigHttpRequest request) {

        log.info("Admin SIP config create — user: {}, sipUsername: {}", userPublicId, request.sipUsername());
        sipConfigService.create(new SaveSipConfigRequest(
                userPublicId, request.sipUsername(), request.sipPassword(), request.displayName()));
        return ResponseEntity.ok(buildStatus(userPublicId));
    }

    // =========================================================================
    // SIP — update
    // =========================================================================

    @PutMapping("/{userPublicId}/sip")
    @Operation(summary = "Update SIP credentials for a user")
    public ResponseEntity<AdminUserTelephonyStatusResponse> updateSipConfig(
            @PathVariable String userPublicId,
            @Valid @RequestBody AdminSaveSipConfigHttpRequest request) {

        User user = userService.getUserByPublicId(userPublicId);
        CommercialSipConfig existing = sipConfigService.getForActor(user);

        log.info("Admin SIP config update — user: {}, configPublicId: {}", userPublicId, existing.getPublicId());
        sipConfigService.update(existing.getPublicId(), new SaveSipConfigRequest(
                userPublicId, request.sipUsername(), request.sipPassword(), request.displayName()));
        return ResponseEntity.ok(buildStatus(userPublicId));
    }

    // =========================================================================
    // SIP — delete
    // =========================================================================

    @DeleteMapping("/{userPublicId}/sip")
    @Operation(summary = "Remove SIP credentials from a user (falls back to global provider)")
    public ResponseEntity<AdminUserTelephonyStatusResponse> deleteSipConfig(
            @PathVariable String userPublicId) {

        User user = userService.getUserByPublicId(userPublicId);
        CommercialSipConfig existing = sipConfigService.getForActor(user);

        log.info("Admin SIP config delete — user: {}, configPublicId: {}", userPublicId, existing.getPublicId());
        sipConfigService.delete(existing.getPublicId());
        return ResponseEntity.ok(buildStatus(userPublicId));
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private AdminUserTelephonyStatusResponse buildStatus(String userPublicId) {
        User user = userService.getUserByPublicId(userPublicId);

        CommercialSipConfig sipConfig = null;
        try {
            sipConfig = sipConfigService.getForActor(user);
        } catch (SipConfigNotFoundException ignored) {}

        String effectiveProvider;
        if (sipConfig != null) {
            effectiveProvider = CallProvider.SIP.name();
        } else if (CallProvider.TWILIO.name().equals(globalProvider)) {
            effectiveProvider = CallProvider.TWILIO.name();
        } else {
            effectiveProvider = "NONE";
        }

        AdminUserTelephonyStatusResponse.SipConfigSummary sipSummary = sipConfig != null
                ? new AdminUserTelephonyStatusResponse.SipConfigSummary(
                        sipConfig.getPublicId(),
                        sipConfig.getSipUsername(),
                        sipConfig.getDisplayName(),
                        sipConfig.getUpdatedAt())
                : null;

        String twilioIdentity = CallProvider.TWILIO.name().equals(effectiveProvider)
                ? user.getPublicId()
                : null;

        return new AdminUserTelephonyStatusResponse(effectiveProvider, globalProvider, sipSummary, twilioIdentity);
    }
}
