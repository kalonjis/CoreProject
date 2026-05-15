package be.steby.CoreProject.dl.entities.crm;

import be.steby.CoreProject.dl.entities.BaseEntity;
import be.steby.CoreProject.dl.enums.crm.CallProvider;
import jakarta.persistence.*;
import lombok.*;

/**
 * Stores the telephony provider configuration for a CRM tenant.
 *
 * <p>Exactly one active {@code TelephonyConfig} determines which
 * {@code TelephonyAdapter} the infrastructure layer selects when a
 * commercial initiates a call. The service layer enforces the uniqueness
 * of the active record.</p>
 *
 * <h3>Credentials</h3>
 * <p>{@code encryptedCredentials} holds a provider-specific JSON payload
 * (e.g., Twilio Account SID + Auth Token, or SIP server URI + credentials)
 * encrypted at the service layer before persistence. It is {@code null}
 * for {@link CallProvider#TEL_URI} which requires no credentials.</p>
 *
 * <h3>Inheritance</h3>
 * <p>Extends {@link BaseEntity} to inherit:</p>
 * <ul>
 *   <li>{@code id} — internal Long PK, never exposed via API</li>
 *   <li>{@code publicId} — UUID exposed in all public endpoints</li>
 *   <li>{@code createdBy}, {@code createdAt}, {@code updatedBy}, {@code updatedAt} — audit fields</li>
 * </ul>
 *
 * @see CallProvider
 * @see CallSession
 */
@Entity
@Table(
    name = "crm_telephony_config",
    indexes = {
        @Index(name = "idx_telephonyconfig_provider", columnList = "provider"),
        @Index(name = "idx_telephonyconfig_active",   columnList = "active")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class TelephonyConfig extends BaseEntity<Long> {

    // =========================================================================
    // Provider
    // =========================================================================

    /**
     * Telephony provider this configuration targets.
     *
     * <p>Required. Determines which adapter is instantiated and which
     * fields of {@code encryptedCredentials} are expected.</p>
     *
     * @see CallProvider
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 15)
    @ToString.Include
    private CallProvider provider;

    // =========================================================================
    // Credentials & identity
    // =========================================================================

    /**
     * Encrypted JSON payload containing provider-specific credentials.
     *
     * <p>Optional — {@code null} for {@link CallProvider#TEL_URI}.
     * Encrypted by the service layer before storage; never returned in API responses.
     * The exact JSON structure depends on the provider:
     * <ul>
     *   <li>TWILIO — {@code { "accountSid": "...", "authToken": "...", "twilioNumber": "..." }}</li>
     *   <li>SIP    — {@code { "serverUri": "...", "username": "...", "password": "..." }}</li>
     * </ul>
     * </p>
     */
    @Column(name = "encrypted_credentials", columnDefinition = "TEXT")
    private String encryptedCredentials;

    /**
     * Phone number displayed to the called party (caller ID).
     *
     * <p>Optional — not used by {@link CallProvider#TEL_URI} which relies
     * on the OS dialer's own caller ID. Should be in E.164 format
     * (e.g., "+32475123456").</p>
     */
    @Column(name = "caller_id", length = 30)
    private String callerId;

    // =========================================================================
    // State
    // =========================================================================

    /**
     * Whether this configuration is currently active.
     *
     * <p>Only one record should be active at a time.
     * The service layer enforces this invariant on create/update.</p>
     */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
