package be.steby.CoreProject.bll.domains.auth.services.twofactor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Service for encrypting and decrypting TOTP secrets.
 * Uses AES encryption for secure storage of TOTP secrets in database.
 * 
 * @author Steby Team
 * @since 2.0.0
 */
@Service
public class TOTPSecretEncryptionService {
    
    private final String encryptionKey;
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";
    
    public TOTPSecretEncryptionService(@Value("${security.totp.encryption-key:MySecretKey123456}") String encryptionKey) {
        // Ensure key is exactly 16 bytes for AES-128
        this.encryptionKey = padKey(encryptionKey);
    }
    
    /**
     * Encrypt a TOTP secret for database storage.
     * 
     * @param plainSecret the plain text TOTP secret
     * @return Base64 encoded encrypted secret
     */
    public String encrypt(String plainSecret) {
        try {
            SecretKey secretKey = new SecretKeySpec(encryptionKey.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] encryptedBytes = cipher.doFinal(plainSecret.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt TOTP secret", e);
        }
    }
    
    /**
     * Decrypt a TOTP secret from database storage.
     * 
     * @param encryptedSecret the Base64 encoded encrypted secret
     * @return plain text TOTP secret
     */
    public String decrypt(String encryptedSecret) {
        try {
            SecretKey secretKey = new SecretKeySpec(encryptionKey.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedSecret);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt TOTP secret", e);
        }
    }
    
    /**
     * Pad the encryption key to exactly 16 bytes for AES-128.
     */
    private String padKey(String key) {
        if (key.length() >= 16) {
            return key.substring(0, 16);
        } else {
            return String.format("%-16s", key).replace(' ', '0');
        }
    }
}