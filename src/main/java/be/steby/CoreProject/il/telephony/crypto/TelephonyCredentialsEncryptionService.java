package be.steby.CoreProject.il.telephony.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256/GCM encryption for telephony credentials stored in the database.
 *
 * <p>Used for both {@link be.steby.CoreProject.dl.entities.crm.TelephonyConfig#getEncryptedCredentials()}
 * and {@link be.steby.CoreProject.dl.entities.crm.CommercialSipConfig#getEncryptedSipPassword()}.
 * The encryption key must be supplied via the {@code TELEPHONY_ENCRYPTION_KEY} environment variable.</p>
 *
 * <h3>Format</h3>
 * <p>Encrypted values are stored as {@code Base64(IV[12] + ciphertext + auth_tag[16])}.
 * A fresh 12-byte IV is generated for each encryption call.</p>
 */
@Service
public class TelephonyCredentialsEncryptionService {

    private static final String ALGORITHM  = "AES/GCM/NoPadding";
    private static final int    IV_LENGTH  = 12;
    private static final int    TAG_LENGTH = 128;

    private final SecretKeySpec secretKey;

    public TelephonyCredentialsEncryptionService(
            @Value("${telephony.encryption-key}") String rawKey) {

        byte[] keyBytes = Arrays.copyOf(rawKey.getBytes(StandardCharsets.UTF_8), 32);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[IV_LENGTH + ciphertext.length];
            System.arraycopy(iv,         0, combined, 0,         IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, IV_LENGTH, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt telephony credentials", e);
        }
    }

    public String decrypt(String encoded) {
        try {
            byte[] combined    = Base64.getDecoder().decode(encoded);
            byte[] iv          = Arrays.copyOf(combined, IV_LENGTH);
            byte[] ciphertext  = Arrays.copyOfRange(combined, IV_LENGTH, combined.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));

            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt telephony credentials", e);
        }
    }
}
