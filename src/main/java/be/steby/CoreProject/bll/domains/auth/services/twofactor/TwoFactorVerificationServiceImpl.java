package be.steby.CoreProject.bll.domains.auth.services.twofactor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * Implementation of TwoFactorVerificationService.
 * Uses SecureRandom for code generation and BCrypt for hashing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TwoFactorVerificationServiceImpl implements TwoFactorVerificationService {
    
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();
    
    @Value("${two-factor-auth.verification-code.length:6}")
    private int codeLength;
    
    @Override
    public String generateVerificationCode() {
        StringBuilder code = new StringBuilder();
        
        for (int i = 0; i < codeLength; i++) {
            code.append(secureRandom.nextInt(10)); // 0-9
        }
        
        String generatedCode = code.toString();
        log.debug("Generated verification code of length: {}", codeLength);
        
        return generatedCode;
    }
    
    @Override
    public String hashVerificationCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Verification code cannot be null or empty");
        }
        
        String hashedCode = passwordEncoder.encode(code);
        log.debug("Verification code hashed successfully");
        
        return hashedCode;
    }
    
    @Override
    public boolean verifyCode(String providedCode, String hashedCode) {
        if (providedCode == null || hashedCode == null) {
            log.warn("Cannot verify code: provided or hashed code is null");
            return false;
        }
        
        // Normalize provided code (remove spaces, etc.)
        String normalizedCode = providedCode.trim().replaceAll("\\s+", "");
        
        // Validate format
        if (!normalizedCode.matches("^\\d{" + codeLength + "}$")) {
            log.warn("Invalid verification code format provided");
            return false;
        }
        
        boolean isValid = passwordEncoder.matches(normalizedCode, hashedCode);
        
        if (isValid) {
            log.debug("Verification code verified successfully");
        } else {
            log.warn("Verification code verification failed");
        }
        
        return isValid;
    }
}