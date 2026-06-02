package be.steby.CoreProject.dl.entities.crm;

import be.steby.CoreProject.dl.entities.BaseEntity;
import be.steby.CoreProject.dl.entities.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * Stores the SIP extension credentials for a single commercial using the SIP adapter.
 *
 * <p>Each commercial who places calls via {@link be.steby.CoreProject.dl.enums.crm.CallProvider#SIP}
 * needs their own extension on the Asterisk server. This entity holds the
 * per-user SIP credentials (username + encrypted password) that SIP.js uses
 * to register against Asterisk's WebSocket interface.</p>
 *
 * <h3>Relation to TelephonyConfig</h3>
 * <p>{@link TelephonyConfig} holds the <em>global</em> Asterisk connection settings
 * (WebSocket URL, SIP domain, outbound context) shared by all commercials.
 * {@code CommercialSipConfig} holds the <em>per-user</em> extension credentials.</p>
 *
 * <h3>Security</h3>
 * <p>{@code encryptedSipPassword} is encrypted at the service layer via
 * {@link be.steby.CoreProject.il.telephony.crypto.TelephonyCredentialsEncryptionService}
 * before persistence. It is never returned in API responses.</p>
 */
@Entity
@Table(
    name = "crm_commercial_sip_config",
    indexes = {
        @Index(name = "idx_commercial_sip_config_user", columnList = "user_id", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class CommercialSipConfig extends BaseEntity<Long> {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * SIP username / extension on the Asterisk server (e.g., {@code "1001"} or {@code "john.doe"}).
     */
    @Column(name = "sip_username", nullable = false, length = 100)
    @ToString.Include
    private String sipUsername;

    /**
     * AES-256/GCM encrypted SIP password. Never exposed in API responses.
     */
    @Column(name = "encrypted_sip_password", nullable = false, columnDefinition = "TEXT")
    private String encryptedSipPassword;

    /**
     * Display name sent in SIP From header (e.g., {@code "John Doe"}).
     * Optional — defaults to the commercial's full name when null.
     */
    @Column(name = "display_name", length = 100)
    private String displayName;
}
