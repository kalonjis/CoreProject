package be.steby.CoreProject.dl.entities.crm;

import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.crm.CrmEntityType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit record capturing a single field change on a CRM entity.
 *
 * <p>This entity is append-only — rows are never updated after insertion.
 * It intentionally does not extend {@code BaseEntity} to avoid the
 * {@code createdBy}/{@code updatedBy} auditing overhead on a log table.</p>
 *
 * <p>The {@code entityPublicId} field is nullable to support GDPR anonymisation:
 * when a contact or lead is erased, all related change log rows are anonymised
 * by nullifying {@code entityPublicId} and replacing {@code oldValue} /
 * {@code newValue} with {@code "[anonymised]"}.</p>
 *
 * <p>Table: {@code crm_change_log}</p>
 */
@Entity
@Table(name = "crm_change_log", indexes = {
        @Index(name = "idx_crm_cl_public_id",  columnList = "public_id"),
        @Index(name = "idx_crm_cl_entity",     columnList = "entity_type, entity_public_id"),
        @Index(name = "idx_crm_cl_changed_at", columnList = "changed_at")
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CrmChangeLog {

    /** Internal surrogate key — never exposed in the API. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Public UUID used in API responses. Generated on first persist. */
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 36)
    private String publicId;

    /** CRM domain this entry belongs to (CONTACT, DEAL, LEAD, ORGANISATION). */
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 20, updatable = false)
    private CrmEntityType entityType;

    /**
     * Public identifier of the changed entity.
     * Nullified during GDPR anonymisation — queries on this field must handle null.
     */
    @Column(name = "entity_public_id", length = 36)
    private String entityPublicId;

    /** Name of the field that changed — e.g. {@code "status"}, {@code "amount"}, {@code "email"}. */
    @Column(name = "field_name", nullable = false, length = 50, updatable = false)
    private String fieldName;

    /**
     * String representation of the value before the change.
     * Null when the field had no prior value (e.g. first assignment).
     * Replaced with {@code "[anonymised]"} during GDPR erasure.
     */
    @Column(name = "old_value", length = 1000)
    private String oldValue;

    /**
     * String representation of the value after the change.
     * Replaced with {@code "[anonymised]"} during GDPR erasure.
     */
    @Column(name = "new_value", length = 1000)
    private String newValue;

    /**
     * User who triggered the change.
     * Null for system-initiated changes (e.g. automated workflows).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id")
    private User changedBy;

    /** UTC timestamp when the change occurred. Set automatically on first persist. */
    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    /**
     * Generates {@code publicId} and sets {@code changedAt} before the first insert.
     * Never called on updates — this entity is append-only.
     */
    @PrePersist
    protected void onCreate() {
        if (this.publicId == null) {
            this.publicId = UUID.randomUUID().toString();
        }
        if (this.changedAt == null) {
            this.changedAt = Instant.now();
        }
    }
}
