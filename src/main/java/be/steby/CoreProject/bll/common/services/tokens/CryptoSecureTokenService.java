package be.steby.CoreProject.bll.common.services.tokens;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Implementation of SecureTokenService using AES-256-GCM encryption.
 *
 * Simplified version for development - clean and secure implementation.
 *
 * Security Features:
 * - AES-256-GCM encryption with authentication
 * - Random IV for each operation
 * - URL-safe base64 encoding
 * - Fast failure for security issues
 *
 * @author Your Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class CryptoSecureTokenService implements SecureTokenService {

    private static final String SECURED_PREFIX = "sec_";
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;  // 96 bits for GCM
    private static final int TAG_LENGTH = 16; // 128 bits authentication

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${security.tokens.encryption.enabled:true}")
    private boolean encryptionEnabled;

    @Value("${security.tokens.encryption.key}")
    private String encryptionKey;

    @Override
    public String secureToken(String plainToken) {
        validateInput(plainToken, "Plain token");

        if (!encryptionEnabled) {
            log.debug("Token encryption disabled, returning plain token");
            return plainToken;
        }

        try {
            return performEncryption(plainToken);
        } catch (Exception e) {
            log.error("Token encryption failed: {}", e.getMessage());
            throw new SecurityException("Failed to secure token", e);
        }
    }

    @Override
    public String recoverToken(String securedToken) {
        validateInput(securedToken, "Secured token");

        // In dev mode, we expect only secured tokens (no backward compatibility)
        if (!isSecured(securedToken)) {
            throw new SecurityException("Invalid token format - expected secured token");
        }

        if (!encryptionEnabled) {
            throw new SecurityException("Cannot recover secured token when encryption is disabled");
        }

        try {
            return performDecryption(securedToken);
        } catch (Exception e) {
            log.error("Token recovery failed: {}", e.getMessage());
            throw new SecurityException("Failed to recover token", e);
        }
    }

    @Override
    public boolean isSecured(String token) {
        return StringUtils.hasText(token) && token.startsWith(SECURED_PREFIX);
    }

    // =========================================================================
    // Private Implementation Methods
    // =========================================================================

    private String performEncryption(String plainToken) throws Exception {
        // Generate random IV
        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);

        // Encrypt with AES-256-GCM
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        SecretKeySpec keySpec = new SecretKeySpec(getKeyBytes(), ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH * 8, iv);

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
        byte[] encryptedData = cipher.doFinal(plainToken.getBytes(StandardCharsets.UTF_8));

        // Combine IV + encrypted data
        byte[] combined = new byte[IV_LENGTH + encryptedData.length];
        System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
        System.arraycopy(encryptedData, 0, combined, IV_LENGTH, encryptedData.length);

        // Return with prefix for identification
        return SECURED_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
    }

    private String performDecryption(String securedToken) throws Exception {
        // Remove prefix and decode
        String base64Data = securedToken.substring(SECURED_PREFIX.length());
        byte[] combined = Base64.getUrlDecoder().decode(base64Data);

        // Extract IV and encrypted data
        byte[] iv = new byte[IV_LENGTH];
        byte[] encryptedData = new byte[combined.length - IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
        System.arraycopy(combined, IV_LENGTH, encryptedData, 0, encryptedData.length);

        // Decrypt with AES-256-GCM
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        SecretKeySpec keySpec = new SecretKeySpec(getKeyBytes(), ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH * 8, iv);

        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
        byte[] decryptedData = cipher.doFinal(encryptedData);

        return new String(decryptedData, StandardCharsets.UTF_8);
    }

    private byte[] getKeyBytes() {
        if (!StringUtils.hasText(encryptionKey)) {
            throw new SecurityException("Encryption key not configured");
        }

        try {
            // Handle base64-encoded keys
            if (isBase64Key(encryptionKey)) {
                byte[] decoded = Base64.getDecoder().decode(encryptionKey);
                return ensureKeyLength(decoded);
            }

            // Handle plain text keys
            byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
            return ensureKeyLength(keyBytes);

        } catch (Exception e) {
            throw new SecurityException("Invalid encryption key format", e);
        }
    }

    private boolean isBase64Key(String key) {
        return key.matches("^[A-Za-z0-9+/]*={0,2}$") && key.length() > 32;
    }

    private byte[] ensureKeyLength(byte[] keyBytes) {
        if (keyBytes.length >= 32) {
            return java.util.Arrays.copyOf(keyBytes, 32);
        } else {
            throw new SecurityException("Encryption key must be at least 32 bytes for AES-256");
        }
    }

    private void validateInput(String input, String fieldName) {
        if (!StringUtils.hasText(input)) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
    }
}