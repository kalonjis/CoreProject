package be.steby.CoreProject.bll.domains.storage.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for the file storage system.
 *
 * <p>Binds to properties under the {@code storage} prefix in application config.
 * Provides typed access to storage settings for services and validators.
 *
 * <p>Example usage:
 * <pre>
 * {@code
 * @Autowired
 * private StorageConfiguration config;
 *
 * long maxSize = config.getCategories().get("avatar").getMaxFileSize();
 * }
 * </pre>
 *
 * @see be.steby.CoreProject.bll.domains.storage.services.FileStorageService
 */
@Component
@ConfigurationProperties(prefix = "storage")
@Getter
@Setter
public class StorageConfiguration {

    /**
     * Base path for file storage.
     * Can be absolute or relative to application root.
     */
    private String basePath = "./uploads";

    /**
     * Global maximum file size in bytes.
     * Default: 50MB
     */
    private long maxFileSize = 52428800L;

    /**
     * Allowed origins for CORS when serving files.
     */
    private List<String> allowedOrigins;

    /**
     * Category-specific configurations.
     * Key: category name (lowercase), Value: category config
     */
    private Map<String, CategoryConfig> categories = new HashMap<>();

    /**
     * Cleanup job settings.
     */
    private CleanupConfig cleanup = new CleanupConfig();

    /**
     * Security settings.
     */
    private SecurityConfig security = new SecurityConfig();

    // =========================================================================
    // Nested Configuration Classes
    // =========================================================================

    /**
     * Configuration for a specific file category.
     */
    @Getter
    @Setter
    public static class CategoryConfig {

        /**
         * Maximum file size for this category (bytes).
         */
        private long maxFileSize;

        /**
         * Allowed MIME types.
         * Use "*\/*" to allow all types.
         */
        private List<String> allowedTypes;

        /**
         * Maximum image width (pixels).
         * Only applicable to image categories.
         */
        private Integer maxWidth;

        /**
         * Maximum image height (pixels).
         * Only applicable to image categories.
         */
        private Integer maxHeight;

        /**
         * Target width for auto-resize.
         * Only applicable to image categories.
         */
        private Integer targetWidth;

        /**
         * Target height for auto-resize.
         * Only applicable to image categories.
         */
        private Integer targetHeight;

        /**
         * Retention hours for temporary files.
         * Only applicable to TEMPORARY category.
         */
        private Integer retentionHours;

        /**
         * Maximum allowed track points in GPX file.
         * Only applicable to GPX_TRACK category.
         * Prevents DoS via extremely large files.
         */
        private Integer maxTrackPoints;


        /**
         * Checks if a MIME type is allowed.
         *
         * @param mimeType the MIME type to check
         * @return true if allowed
         */
        public boolean isTypeAllowed(String mimeType) {
            if (allowedTypes == null || allowedTypes.isEmpty()) {
                return false;
            }
            // Wildcard allows all
            if (allowedTypes.contains("*/*")) {
                return true;
            }
            return allowedTypes.stream()
                    .anyMatch(type -> type.equalsIgnoreCase(mimeType));
        }
    }

    /**
     * Configuration for cleanup scheduled job.
     */
    @Getter
    @Setter
    public static class CleanupConfig {

        /**
         * Enable/disable cleanup job.
         */
        private boolean enabled = true;

        /**
         * Cron expression for cleanup schedule.
         */
        private String cron = "0 0 3 * * ?";

        /**
         * Days to keep deleted files before purging.
         */
        private int retentionDays = 30;

        /**
         * Hours to keep temporary files.
         */
        private int tempRetentionHours = 24;
    }

    /**
     * Security-related configuration.
     */
    @Getter
    @Setter
    public static class SecurityConfig {

        /**
         * Generate and store file checksum.
         */
        private boolean checksumEnabled = true;

        /**
         * Validate MIME type from content (not just extension).
         */
        private boolean strictMimeValidation = true;

        /**
         * Block executable files.
         */
        private boolean blockExecutables = true;

        /**
         * List of blocked file extensions.
         */
        private List<String> blockedExtensions;

        /**
         * Checks if an extension is blocked.
         *
         * @param extension the extension (without dot)
         * @return true if blocked
         */
        public boolean isExtensionBlocked(String extension) {
            if (blockedExtensions == null || extension == null) {
                return false;
            }
            return blockedExtensions.stream()
                    .anyMatch(blocked -> blocked.equalsIgnoreCase(extension));
        }
    }

    // =========================================================================
    // Convenience Methods
    // =========================================================================

    /**
     * Gets configuration for a specific category.
     *
     * @param categoryName category name (case-insensitive)
     * @return category config or null if not found
     */
    public CategoryConfig getCategoryConfig(String categoryName) {
        if (categoryName == null || categories == null) {
            return null;
        }
        // Try exact match first, then lowercase
        CategoryConfig config = categories.get(categoryName);
        if (config == null) {
            config = categories.get(categoryName.toLowerCase().replace("_", "-"));
        }
        return config;
    }

    /**
     * Gets max file size for a category, falling back to global max.
     *
     * @param categoryName category name
     * @return max file size in bytes
     */
    public long getMaxFileSizeForCategory(String categoryName) {
        CategoryConfig config = getCategoryConfig(categoryName);
        if (config != null && config.getMaxFileSize() > 0) {
            return config.getMaxFileSize();
        }
        return maxFileSize;
    }
}