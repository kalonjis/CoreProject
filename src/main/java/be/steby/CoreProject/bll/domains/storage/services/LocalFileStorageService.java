package be.steby.CoreProject.bll.domains.storage.services;

import be.steby.CoreProject.bll.domains.storage.config.StorageConfiguration;
import be.steby.CoreProject.bll.domains.storage.exceptions.FileNotFoundException;
import be.steby.CoreProject.bll.domains.storage.exceptions.FileStorageException;
import be.steby.CoreProject.bll.domains.storage.models.FileUploadResult;
import be.steby.CoreProject.dal.repositories.StoredFileRepository;
import be.steby.CoreProject.dl.entities.StoredFile;
import be.steby.CoreProject.dl.enums.FileCategory;
import be.steby.CoreProject.dl.enums.FileStatus;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Local filesystem implementation of {@link FileStorageService}.
 *
 * <p>Stores files on the local filesystem with the following structure:
 * <pre>
 * {base-path}/
 *   ├── avatars/
 *   │   └── 2025/01/
 *   │       └── abc123-def456.jpg
 *   ├── documents/
 *   │   └── 2025/01/
 *   │       └── xyz789.pdf
 *   └── products/
 *       └── 2025/01/
 *           └── prod123.png
 * </pre>
 *
 * <p>Features:
 * <ul>
 *   <li>UUID-based filenames (prevents conflicts and path traversal)</li>
 *   <li>Year/month subdirectories (prevents too many files per directory)</li>
 *   <li>SHA-256 checksums for integrity verification</li>
 *   <li>Image dimension extraction</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    private final StorageConfiguration storageConfig;
    private final FileValidationService validationService;
    private final StoredFileRepository fileRepository;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        rootLocation = Paths.get(storageConfig.getBasePath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
            log.info("File storage initialized at: {}", rootLocation);
        } catch (IOException e) {
            throw new FileStorageException("Could not initialize storage location", e);
        }
    }

    // =========================================================================
    // Store Operations
    // =========================================================================

    @Override
    @Transactional
    public FileUploadResult store(MultipartFile file, FileCategory category,
                                   String ownerPublicId, String ownerType) {
        return store(file, category, ownerPublicId, ownerType, null);
    }

    @Override
    @Transactional
    public FileUploadResult store(MultipartFile file, FileCategory category,
                                   String ownerPublicId, String ownerType, String description) {
        // 1. Validate file
        validationService.validate(file, category);

        // 2. Generate storage path
        String storedFilename = generateStoredFilename(file.getOriginalFilename());
        String storagePath = buildStoragePath(category, storedFilename);
        Path targetPath = rootLocation.resolve(storagePath).normalize();

        // 3. Ensure directory exists
        try {
            Files.createDirectories(targetPath.getParent());
        } catch (IOException e) {
            throw new FileStorageException("Failed to create storage directory", e);
        }

        // 4. Calculate checksum (if enabled)
        String checksum = null;
        if (storageConfig.getSecurity().isChecksumEnabled()) {
            checksum = calculateChecksum(file);
        }

        // 5. Extract image dimensions (if applicable)
        Integer imageWidth = null;
        Integer imageHeight = null;
        String contentType = file.getContentType();
        if (contentType != null && contentType.startsWith("image/")) {
            int[] dimensions = extractImageDimensions(file);
            if (dimensions != null) {
                imageWidth = dimensions[0];
                imageHeight = dimensions[1];
            }
        }

        // 6. Store physical file
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("File stored at: {}", targetPath);
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file: " + file.getOriginalFilename(), e);
        }

        // 7. Save metadata to database
        StoredFile storedFile = StoredFile.builder()
                .originalFilename(file.getOriginalFilename())
                .storedFilename(storedFilename)
                .storagePath(storagePath)
                .contentType(contentType)
                .fileSize(file.getSize())
                .extension(getExtension(file.getOriginalFilename()))
                .category(category)
                .status(FileStatus.ACTIVE)
                .ownerPublicId(ownerPublicId)
                .ownerType(ownerType)
                .checksum(checksum)
                .imageWidth(imageWidth)
                .imageHeight(imageHeight)
                .description(description)
                .build();

        storedFile = fileRepository.save(storedFile);

        log.info("File uploaded: publicId={}, category={}, size={}, owner={}",
                storedFile.getPublicId(), category, file.getSize(), ownerPublicId);

        // 8. Build and return result
        return FileUploadResult.builder()
                .publicId(storedFile.getPublicId())
                .originalFilename(file.getOriginalFilename())
                .fileSize(file.getSize())
                .contentType(contentType)
                .category(category)
                .accessUrl(buildAccessUrl(storedFile.getPublicId(), category))
                .checksum(checksum)
                .imageWidth(imageWidth)
                .imageHeight(imageHeight)
                .build();
    }

    // =========================================================================
    // Load Operations
    // =========================================================================

    @Override
    public Resource loadAsResource(String publicId) {
        StoredFile storedFile = findActiveFile(publicId);
        
        try {
            Path filePath = rootLocation.resolve(storedFile.getStoragePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                log.error("File not found on disk: {}", filePath);
                throw new FileNotFoundException("File not found: " + publicId);
            }
        } catch (MalformedURLException e) {
            throw new FileNotFoundException("File not found: " + publicId, e);
        }
    }

    // =========================================================================
    // Delete Operations
    // =========================================================================

    @Override
    @Transactional
    public void delete(String publicId) {
        StoredFile storedFile = findActiveFile(publicId);
        storedFile.markDeleted();
        fileRepository.save(storedFile);
        
        log.info("File marked for deletion: publicId={}", publicId);
    }

    @Override
    @Transactional
    public void deletePermanently(String publicId) {
        StoredFile storedFile = fileRepository.findByPublicId(publicId)
                .orElseThrow(() -> new FileNotFoundException(publicId));

        // Delete physical file
        try {
            Path filePath = rootLocation.resolve(storedFile.getStoragePath()).normalize();
            Files.deleteIfExists(filePath);
            log.debug("Physical file deleted: {}", filePath);
        } catch (IOException e) {
            log.warn("Failed to delete physical file: {}", e.getMessage());
        }

        // Delete metadata
        fileRepository.delete(storedFile);
        
        log.info("File permanently deleted: publicId={}", publicId);
    }

    // =========================================================================
    // Query Operations
    // =========================================================================

    @Override
    public boolean exists(String publicId) {
        return fileRepository.findByPublicIdAndStatus(publicId, FileStatus.ACTIVE).isPresent();
    }

    @Override
    public String getAccessUrl(String publicId) {
        StoredFile storedFile = findActiveFile(publicId);
        return buildAccessUrl(publicId, storedFile.getCategory());
    }

    // =========================================================================
    // Private Helper Methods
    // =========================================================================

    private StoredFile findActiveFile(String publicId) {
        return fileRepository.findByPublicIdAndStatus(publicId, FileStatus.ACTIVE)
                .orElseThrow(() -> new FileNotFoundException(publicId));
    }

    private String generateStoredFilename(String originalFilename) {
        String extension = getExtension(originalFilename);
        String uuid = UUID.randomUUID().toString();
        return extension != null ? uuid + "." + extension : uuid;
    }

    private String buildStoragePath(FileCategory category, String filename) {
        LocalDate now = LocalDate.now();
        String yearMonth = now.format(DateTimeFormatter.ofPattern("yyyy/MM"));
        return String.format("%s/%s/%s", category.getSubdirectory(), yearMonth, filename);
    }

    private String buildAccessUrl(String publicId, FileCategory category) {
        // URL pattern: /api/files/{publicId}
        return "/api/files/" + publicId;
    }

    private String getExtension(String filename) {
        if (filename == null) return null;
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1).toLowerCase();
        }
        return null;
    }

    private String calculateChecksum(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | IOException e) {
            log.warn("Failed to calculate checksum: {}", e.getMessage());
            return null;
        }
    }

    private int[] extractImageDimensions(MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image != null) {
                return new int[]{image.getWidth(), image.getHeight()};
            }
        } catch (IOException e) {
            log.warn("Failed to extract image dimensions: {}", e.getMessage());
        }
        return null;
    }
}