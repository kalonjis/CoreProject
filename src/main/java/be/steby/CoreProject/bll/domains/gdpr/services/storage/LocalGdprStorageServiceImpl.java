package be.steby.CoreProject.bll.domains.gdpr.services.storage;

import be.steby.CoreProject.bll.domains.gdpr.exceptions.GdprStorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Local filesystem implementation of {@link GdprStorageService}.
 * Activated when: {@code app.gdpr.storage.provider=local}
 *
 * <p>Archive structure on disk:
 * <pre>
 * {app.gdpr.storage.local.path}/
 *   └── {fileId}.zip
 * </pre>
 */
@Slf4j
public class LocalGdprStorageServiceImpl implements GdprStorageService {

    private final Path storageDir;

    public LocalGdprStorageServiceImpl(@Value("${app.gdpr.storage.local.path}") String path) {
        this.storageDir = Paths.get(path).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageDir);
            log.info("GDPR local storage initialized at: {}", this.storageDir);
        } catch (IOException e) {
            throw new GdprStorageException("Cannot initialize GDPR local storage at: " + path, e);
        }
    }

    @Override
    public void save(String fileId, byte[] data) {
        try {
            Files.write(resolve(fileId), data);
            log.debug("GDPR archive saved locally: {}", fileId);
        } catch (IOException e) {
            throw new GdprStorageException("Failed to save GDPR archive: " + fileId, e);
        }
    }

    @Override
    public String getDownloadUrl(String fileId) {
        // In local mode, no external URL is generated.
        // The controller detects isRemote() = false and serves the file directly.
        return fileId;
    }

    @Override
    public byte[] load(String fileId) {
        try {
            Path p = resolve(fileId);
            if (!Files.exists(p)) {
                throw new GdprStorageException("GDPR archive not found: " + fileId);
            }
            return Files.readAllBytes(p);
        } catch (IOException e) {
            throw new GdprStorageException("Failed to load GDPR archive: " + fileId, e);
        }
    }

    @Override
    public void delete(String fileId) {
        try {
            Files.deleteIfExists(resolve(fileId));
            log.debug("GDPR archive deleted locally: {}", fileId);
        } catch (IOException e) {
            log.warn("Failed to delete GDPR archive: {}", fileId, e);
        }
    }

    @Override
    public boolean isRemote() {
        return false;
    }

    // =========================================================================
    // Private
    // =========================================================================

    private Path resolve(String fileId) {
        return storageDir.resolve(fileId + ".zip").normalize();
    }
}