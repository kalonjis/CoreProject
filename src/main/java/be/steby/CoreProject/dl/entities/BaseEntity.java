package be.steby.CoreProject.dl.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Base entity class providing common fields and functionality for all entities.
 *
 * Features:
 * - Auto-generated primary key (Long ID for performance)
 * - Auto-generated public UUID for secure API exposure
 * - Audit fields (created/updated by/at)
 * - Automatic UUID generation on entity creation
 *
 * Security:
 * - Internal Long ID: Used for database relations and performance
 * - Public UUID: Used for API endpoints to prevent enumeration attacks
 *
 * @param <T> The type of the primary key (typically Long)
 */
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@MappedSuperclass
@Getter
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity<T extends Serializable> {

    /**
     * Primary key for database operations.
     * Auto-generated, used internally for performance and relations.
     * NEVER exposed in public APIs.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private T id;

    /**
     * Public UUID for secure API exposure.
     *
     * - Used in all public API endpoints instead of the internal ID
     * - Prevents enumeration attacks (unpredictable)
     * - Unique across the entire system
     * - Generated automatically on entity creation
     *
     * Security benefit: URLs like /api/users/{uuid} instead of /api/users/123
     * Prevents attackers from guessing sequential IDs
     */
    @Column(name = "public_id", nullable = true, unique = true, updatable = false, length = 256)
    @Setter
    private String publicId;

    /**
     * Username of the user who created this entity.
     * Populated automatically by Spring Data JPA Auditing.
     */
    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 50)
    protected String createdBy;

    /**
     * Timestamp when this entity was created.
     * Set automatically by Hibernate on insert.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    @Setter
    private Instant createdAt;

    /**
     * Username of the user who last updated this entity.
     * Updated automatically by Spring Data JPA Auditing.
     */
    @LastModifiedBy
    @Column(name = "updated_by", insertable = false, length = 50)
    protected String updatedBy;

    /**
     * Timestamp when this entity was last updated.
     * Updated automatically by Hibernate on every update.
     */
    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    @Setter
    private Instant updatedAt;

    /**
     * Automatically generates a public UUID before entity persistence.
     *
     * Called by JPA before INSERT operations.
     * Only generates UUID for entities that should have one (excludes tokens).
     */
    @PrePersist
    protected void generatePublicId() {
        if (this.publicId == null && shouldGeneratePublicId()) {
            this.publicId = UUID.randomUUID().toString();
        }
    }

    /**
     * Determines if this entity should have a public UUID generated.
     *
     * Override this method in subclasses to control UUID generation.
     * For example, token entities might return false since they already
     * have their own secure token field.
     *
     * @return true if a public UUID should be generated, false otherwise
     */
    protected boolean shouldGeneratePublicId() {
        return true;
    }

    /**
     * Checks if this entity has a public ID assigned.
     *
     * @return true if publicId is not null and not empty
     */
    public boolean hasPublicId() {
        return this.publicId != null && !this.publicId.trim().isEmpty();
    }

    /**
     * Checks if this entity is persisted (has an internal ID).
     *
     * @return true if the entity has been saved to the database
     */
    public boolean isPersisted() {
        return this.id != null;
    }

    /**
     * Checks if this entity was created by the specified user.
     *
     * @param username the username to check
     * @return true if the entity was created by the specified user
     */
    public boolean isCreatedBy(String username) {
        return this.createdBy != null && this.createdBy.equals(username);
    }

    /**
     * Checks if this entity was last updated by the specified user.
     *
     * @param username the username to check
     * @return true if the entity was last updated by the specified user
     */
    public boolean isUpdatedBy(String username) {
        return this.updatedBy != null && this.updatedBy.equals(username);
    }

    /**
     * Gets the age of this entity in milliseconds.
     *
     * @return the number of milliseconds since creation
     */
    public long getAgeInMillis() {
        return this.createdAt != null ?
                Instant.now().toEpochMilli() - this.createdAt.toEpochMilli() : 0L;
    }

    /**
     * Gets the time since last update in milliseconds.
     *
     * @return the number of milliseconds since last update
     */
    public long getTimeSinceLastUpdateInMillis() {
        return this.updatedAt != null ?
                Instant.now().toEpochMilli() - this.updatedAt.toEpochMilli() : 0L;
    }
}