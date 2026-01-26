package be.steby.CoreProject.dal.repositories;

import be.steby.CoreProject.dl.entities.StoredFile;
import be.steby.CoreProject.dl.enums.FileCategory;
import be.steby.CoreProject.dl.enums.FileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link StoredFile} entity operations.
 *
 * <p>Provides CRUD operations and domain-specific queries for file metadata.
 * Follows KISS principle with only essential queries.
 *
 * @see StoredFile
 */
@Repository
public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {

    // =========================================================================
    // Basic Lookups
    // =========================================================================

    /**
     * Finds a file by its public ID.
     *
     * @param publicId the public UUID
     * @return the file if found
     */
    Optional<StoredFile> findByPublicId(String publicId);

    /**
     * Finds an active file by its public ID.
     *
     * @param publicId the public UUID
     * @return the file if found and active
     */
    Optional<StoredFile> findByPublicIdAndStatus(String publicId, FileStatus status);

    /**
     * Checks if a file exists by public ID.
     *
     * @param publicId the public UUID
     * @return true if exists
     */
    boolean existsByPublicId(String publicId);

    // =========================================================================
    // Owner-Based Queries
    // =========================================================================

    /**
     * Finds all active files for an owner.
     *
     * @param ownerPublicId owner's public ID
     * @param ownerType     type of owner (USER, PRODUCT, etc.)
     * @return list of active files
     */
    @Query("SELECT f FROM StoredFile f WHERE f.ownerPublicId = :ownerId " +
           "AND f.ownerType = :ownerType AND f.status = 'ACTIVE' ORDER BY f.createdAt DESC")
    List<StoredFile> findActiveByOwner(
            @Param("ownerId") String ownerPublicId,
            @Param("ownerType") String ownerType
    );

    /**
     * Finds all files for an owner by category.
     *
     * @param ownerPublicId owner's public ID
     * @param ownerType     type of owner
     * @param category      file category
     * @return list of files
     */
    List<StoredFile> findByOwnerPublicIdAndOwnerTypeAndCategoryAndStatus(
            String ownerPublicId,
            String ownerType,
            FileCategory category,
            FileStatus status
    );

    /**
     * Counts files for an owner.
     *
     * @param ownerPublicId owner's public ID
     * @param ownerType     type of owner
     * @return file count
     */
    long countByOwnerPublicIdAndOwnerTypeAndStatus(
            String ownerPublicId,
            String ownerType,
            FileStatus status
    );

    /**
     * Calculates total storage used by an owner.
     *
     * @param ownerPublicId owner's public ID
     * @param ownerType     type of owner
     * @return total bytes used
     */
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM StoredFile f " +
           "WHERE f.ownerPublicId = :ownerId AND f.ownerType = :ownerType AND f.status = 'ACTIVE'")
    Long calculateStorageUsed(
            @Param("ownerId") String ownerPublicId,
            @Param("ownerType") String ownerType
    );

    // =========================================================================
    // Checksum-Based Queries (Duplicate Detection)
    // =========================================================================

    /**
     * Finds files with the same checksum.
     * Used for duplicate detection.
     *
     * @param checksum SHA-256 hash
     * @return list of files with same content
     */
    List<StoredFile> findByChecksumAndStatus(String checksum, FileStatus status);

    // =========================================================================
    // Cleanup Queries
    // =========================================================================

    /**
     * Finds files marked for deletion before a given date.
     * Used by cleanup scheduled job.
     *
     * @param before delete files marked before this instant
     * @return files to physically delete
     */
    @Query("SELECT f FROM StoredFile f WHERE f.status = 'DELETED' AND f.deletedAt < :before")
    List<StoredFile> findFilesToPurge(@Param("before") Instant before);

    /**
     * Finds orphan temporary files older than given date.
     *
     * @param before files created before this instant
     * @return temporary files to clean up
     */
    @Query("SELECT f FROM StoredFile f WHERE f.category = 'TEMPORARY' AND f.createdAt < :before")
    List<StoredFile> findExpiredTemporaryFiles(@Param("before") Instant before);

    /**
     * Bulk soft-delete all files for an owner.
     * Used when user account is deleted.
     *
     * @param ownerPublicId owner's public ID
     * @param ownerType     type of owner
     * @param deletedAt     deletion timestamp
     * @return number of files marked
     */
    @Modifying
    @Query("UPDATE StoredFile f SET f.status = 'DELETED', f.deletedAt = :deletedAt " +
           "WHERE f.ownerPublicId = :ownerId AND f.ownerType = :ownerType AND f.status = 'ACTIVE'")
    int markAllOwnerFilesDeleted(
            @Param("ownerId") String ownerPublicId,
            @Param("ownerType") String ownerType,
            @Param("deletedAt") Instant deletedAt
    );
}