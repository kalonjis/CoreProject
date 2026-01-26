package be.steby.CoreProject.bll.domains.storage.services;

import be.steby.CoreProject.bll.domains.storage.exceptions.FileNotFoundException;
import be.steby.CoreProject.dal.repositories.StoredFileRepository;
import be.steby.CoreProject.dl.entities.StoredFile;
import be.steby.CoreProject.dl.enums.FileCategory;
import be.steby.CoreProject.dl.enums.FileStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for file metadata operations.
 *
 * <p>Provides CRUD operations on file metadata without
 * touching the physical file storage.
 *
 * <p>Use cases:
 * <ul>
 *   <li>Query files by owner</li>
 *   <li>Update file description</li>
 *   <li>Check storage quota</li>
 *   <li>Find duplicates by checksum</li>
 * </ul>
 *
 * @see StoredFile
 * @see FileStorageService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileMetadataService {

    private final StoredFileRepository fileRepository;

    // =========================================================================
    // Query Operations
    // =========================================================================

    /**
     * Finds a file by its public ID.
     *
     * @param publicId the file's public ID
     * @return the file if found
     */
    public Optional<StoredFile> findByPublicId(String publicId) {
        return fileRepository.findByPublicId(publicId);
    }

    /**
     * Gets a file by public ID or throws.
     *
     * @param publicId the file's public ID
     * @return the file
     * @throws FileNotFoundException if not found
     */
    public StoredFile getByPublicId(String publicId) {
        return fileRepository.findByPublicIdAndStatus(publicId, FileStatus.ACTIVE)
                .orElseThrow(() -> new FileNotFoundException(publicId));
    }

    /**
     * Finds all active files for an owner.
     *
     * @param ownerPublicId owner's public ID
     * @param ownerType     type of owner
     * @return list of files
     */
    public List<StoredFile> findByOwner(String ownerPublicId, String ownerType) {
        return fileRepository.findActiveByOwner(ownerPublicId, ownerType);
    }

    /**
     * Finds files by owner and category.
     *
     * @param ownerPublicId owner's public ID
     * @param ownerType     type of owner
     * @param category      file category
     * @return list of files
     */
    public List<StoredFile> findByOwnerAndCategory(String ownerPublicId, String ownerType,
                                                    FileCategory category) {
        return fileRepository.findByOwnerPublicIdAndOwnerTypeAndCategoryAndStatus(
                ownerPublicId, ownerType, category, FileStatus.ACTIVE);
    }

    /**
     * Counts files for an owner.
     *
     * @param ownerPublicId owner's public ID
     * @param ownerType     type of owner
     * @return file count
     */
    public long countByOwner(String ownerPublicId, String ownerType) {
        return fileRepository.countByOwnerPublicIdAndOwnerTypeAndStatus(
                ownerPublicId, ownerType, FileStatus.ACTIVE);
    }

    // =========================================================================
    // Storage Quota
    // =========================================================================

    /**
     * Calculates total storage used by an owner.
     *
     * @param ownerPublicId owner's public ID
     * @param ownerType     type of owner
     * @return total bytes used
     */
    public long calculateStorageUsed(String ownerPublicId, String ownerType) {
        Long used = fileRepository.calculateStorageUsed(ownerPublicId, ownerType);
        return used != null ? used : 0L;
    }

    /**
     * Checks if adding a file would exceed quota.
     *
     * @param ownerPublicId owner's public ID
     * @param ownerType     type of owner
     * @param fileSize      size of file to add
     * @param quotaLimit    quota limit in bytes
     * @return true if quota would be exceeded
     */
    public boolean wouldExceedQuota(String ownerPublicId, String ownerType,
                                    long fileSize, long quotaLimit) {
        long currentUsage = calculateStorageUsed(ownerPublicId, ownerType);
        return (currentUsage + fileSize) > quotaLimit;
    }

    // =========================================================================
    // Update Operations
    // =========================================================================

    /**
     * Updates file description.
     *
     * @param publicId    the file's public ID
     * @param description new description
     * @return updated file
     */
    @Transactional
    public StoredFile updateDescription(String publicId, String description) {
        StoredFile file = getByPublicId(publicId);
        file.setDescription(description);
        return fileRepository.save(file);
    }

    /**
     * Archives a file (soft delete alternative).
     *
     * @param publicId the file's public ID
     * @return archived file
     */
    @Transactional
    public StoredFile archive(String publicId) {
        StoredFile file = getByPublicId(publicId);
        file.setStatus(FileStatus.ARCHIVED);
        log.info("File archived: publicId={}", publicId);
        return fileRepository.save(file);
    }

    /**
     * Restores an archived file.
     *
     * @param publicId the file's public ID
     * @return restored file
     */
    @Transactional
    public StoredFile restore(String publicId) {
        StoredFile file = fileRepository.findByPublicIdAndStatus(publicId, FileStatus.ARCHIVED)
                .orElseThrow(() -> new FileNotFoundException(publicId));
        file.setStatus(FileStatus.ACTIVE);
        log.info("File restored: publicId={}", publicId);
        return fileRepository.save(file);
    }

    // =========================================================================
    // Duplicate Detection
    // =========================================================================

    /**
     * Finds files with the same checksum.
     *
     * @param checksum SHA-256 hash
     * @return list of duplicate files
     */
    public List<StoredFile> findByChecksum(String checksum) {
        if (checksum == null || checksum.isBlank()) {
            return List.of();
        }
        return fileRepository.findByChecksumAndStatus(checksum, FileStatus.ACTIVE);
    }

    /**
     * Checks if a file with the same checksum already exists.
     *
     * @param checksum SHA-256 hash
     * @return true if duplicate exists
     */
    public boolean isDuplicate(String checksum) {
        return !findByChecksum(checksum).isEmpty();
    }
}