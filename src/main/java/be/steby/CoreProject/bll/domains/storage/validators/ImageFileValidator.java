package be.steby.CoreProject.bll.domains.storage.validators;

import be.steby.CoreProject.bll.domains.storage.config.StorageConfiguration;
import be.steby.CoreProject.bll.domains.storage.config.StorageConfiguration.CategoryConfig;
import be.steby.CoreProject.dl.enums.FileCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validator for image files.
 *
 * <p>Validates:
 * <ul>
 *   <li>MIME type (must be an image)</li>
 *   <li>File size</li>
 *   <li>Image dimensions (width/height)</li>
 *   <li>Image readability (corrupted files)</li>
 * </ul>
 *
 * <p>Supports categories: AVATAR, PRODUCT_IMAGE
 *
 * @see FileValidator
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ImageFileValidator implements FileValidator {

    private final StorageConfiguration storageConfig;

    private static final Set<FileCategory> SUPPORTED_CATEGORIES = Set.of(
            FileCategory.AVATAR,
            FileCategory.PRODUCT_IMAGE
    );

    private static final Set<String> IMAGE_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    @Override
    public boolean supports(FileCategory category) {
        return SUPPORTED_CATEGORIES.contains(category);
    }

    @Override
    public int getPriority() {
        return 10; // High priority for image validation
    }

    @Override
    public List<String> validate(MultipartFile file, FileCategory category) {
        List<String> errors = new ArrayList<>();

        // Get category config
        String configKey = category.name().toLowerCase().replace("_", "-");
        CategoryConfig config = storageConfig.getCategoryConfig(configKey);

        // 1. Check MIME type
        String contentType = file.getContentType();
        if (contentType == null || !isImageType(contentType)) {
            errors.add("File must be an image (JPEG, PNG, WebP, or GIF)");
            return errors; // No point checking further
        }

        // 2. Check against allowed types for category
        if (config != null && !config.isTypeAllowed(contentType)) {
            errors.add("Image type '" + contentType + "' is not allowed for " + category);
        }

        // 3. Check file size
        long maxSize = config != null ? config.getMaxFileSize() : storageConfig.getMaxFileSize();
        if (file.getSize() > maxSize) {
            errors.add(String.format("Image too large. Maximum: %s, Actual: %s",
                    formatBytes(maxSize), formatBytes(file.getSize())));
        }

        // 4. Read and validate image dimensions
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                errors.add("Unable to read image. File may be corrupted.");
                return errors;
            }

            int width = image.getWidth();
            int height = image.getHeight();

            // Check max dimensions
            if (config != null) {
                if (config.getMaxWidth() != null && width > config.getMaxWidth()) {
                    errors.add(String.format("Image width exceeds maximum. Max: %dpx, Actual: %dpx",
                            config.getMaxWidth(), width));
                }
                if (config.getMaxHeight() != null && height > config.getMaxHeight()) {
                    errors.add(String.format("Image height exceeds maximum. Max: %dpx, Actual: %dpx",
                            config.getMaxHeight(), height));
                }
            }

            // Log validation success
            log.debug("Image validated: {}x{}, {} bytes, type: {}",
                    width, height, file.getSize(), contentType);

        } catch (IOException e) {
            log.warn("Failed to read image for validation: {}", e.getMessage());
            errors.add("Unable to process image. File may be corrupted.");
        }

        return errors;
    }

    /**
     * Checks if MIME type is an image type.
     */
    private boolean isImageType(String contentType) {
        return contentType != null && 
               (IMAGE_MIME_TYPES.contains(contentType.toLowerCase()) ||
                contentType.toLowerCase().startsWith("image/"));
    }

    /**
     * Formats bytes to human-readable string.
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}