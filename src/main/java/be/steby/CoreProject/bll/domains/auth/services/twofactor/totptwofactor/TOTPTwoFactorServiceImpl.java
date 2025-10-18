package be.steby.CoreProject.bll.domains.auth.services.twofactor.totptwofactor;

import be.steby.CoreProject.bll.common.services.tokens.SecureTokenService;
import be.steby.CoreProject.bll.domains.auth.config.TOTPProperties;
import be.steby.CoreProject.bll.domains.auth.events.TwoFactorEnabledEvent;
import be.steby.CoreProject.bll.domains.auth.exceptions.InvalidTwoFactorCodeException;
import be.steby.CoreProject.bll.domains.auth.exceptions.TwoFactorAlreadyEnabledException;
import be.steby.CoreProject.bll.domains.auth.exceptions.TwoFactorSetupNotFoundException;
import be.steby.CoreProject.bll.domains.user.services.UserService;
import be.steby.CoreProject.dal.repositories.TwoFactorAuthRepository;
import be.steby.CoreProject.dl.entities.TwoFactorAuth;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.enums.TwoFactorType;
import be.steby.CoreProject.pl.domains.auth.models.responses.TOTPSetupResponse;
import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base32;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.spec.SecretKeySpec;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.imageio.ImageIO;

/**
 * Implementation of TOTPTwoFactorService.
 * Handles all TOTP-specific 2FA logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TOTPTwoFactorServiceImpl implements TOTPTwoFactorService {

    private final UserService userService;
    private final TwoFactorAuthRepository twoFactorAuthRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SecureTokenService secureTokenService;
    private final TOTPProperties totpProperties;
    
    @Override
    @Transactional
    public TOTPSetupResponse setupTOTPTwoFactor(User user) {
        // If user is null, get authenticated user
        if (user == null) {
            user = userService.getAuthenticatedUser();
        }
        
        log.info("Setting up TOTP 2FA for user: {}", user.getUsername());
        
        // Check if TOTP already exists
        if (twoFactorAuthRepository.existsByUserAndTypeAndEnabledTrue(user, TwoFactorType.TOTP)) {
            log.warn("TOTP 2FA already enabled for user: {}", user.getUsername());
            throw TwoFactorAlreadyEnabledException.forType("TOTP");
        }
        
        // Generate TOTP secret
        String secret = generateTOTPSecret();
        
        // Encrypt secret before storage
        String encryptedSecret = secureTokenService.secureToken(secret);
        
        // Create TOTP entity (not enabled yet - waiting for verification)
        TwoFactorAuth totpTwoFactor = TwoFactorAuth.builder()
            .user(user)
            .type(TwoFactorType.TOTP)
            .secret(encryptedSecret)
            .enabled(false) // Will be enabled after verification
            .isPrimary(false)
            .failedAttempts(0)
            .label("Authenticator App")
            .build();
        
        twoFactorAuthRepository.save(totpTwoFactor);
        
        // Generate QR code data
        String accountName = user.getEmail();
        String issuer = totpProperties.getIssuer();
        String qrCodeUrl = generateTOTPUrl(secret, accountName, issuer);
        String qrCodeImage = generateQRCodeImage(qrCodeUrl);
        
        log.info("TOTP setup data generated for user: {}", user.getUsername());
        
        return TOTPSetupResponse.create(secret, qrCodeUrl, qrCodeImage, accountName, issuer);
    }
    
    @Override
    @Transactional
    public void verifyTOTPSetup(User user, String code) {
        // If user is null, get authenticated user
        if (user == null) {
            user = userService.getAuthenticatedUser();
        }
        
        log.info("Verifying TOTP setup for user: {}", user.getUsername());
        
        // Find pending TOTP setup
        TwoFactorAuth totpAuth = twoFactorAuthRepository
            .findByUserAndTypeAndEnabledFalse(user, TwoFactorType.TOTP)
            .orElseThrow(() -> TwoFactorSetupNotFoundException.forTOTP());
        
        // Decrypt secret
        String secret = secureTokenService.recoverToken(totpAuth.getSecret());
        
        // Verify code
        if (!verifyTOTPCodeInternal(secret, code)) {
            log.warn("Invalid TOTP code during setup for user: {}", user.getUsername());
            throw InvalidTwoFactorCodeException.forTOTP();
        }
        
        // Enable TOTP and set as primary
        disableExistingPrimaryMethod(user);
        
        totpAuth.setEnabled(true);
        totpAuth.setIsPrimary(true);
        totpAuth.setEnabledAt(Instant.now());
        twoFactorAuthRepository.save(totpAuth);
        
        // Publish event for email notification
        eventPublisher.publishEvent(new TwoFactorEnabledEvent(user, TwoFactorType.TOTP));
        
        log.info("TOTP 2FA enabled successfully for user: {}", user.getUsername());
    }
    
    @Override
    public boolean verifyCode(String code, User user) {
        log.debug("Verifying TOTP code for user: {}", user.getUsername());
        
        return twoFactorAuthRepository
            .findByUserAndTypeAndEnabledTrue(user, TwoFactorType.TOTP)
            .map(totpAuth -> {
                String secret = secureTokenService.recoverToken(totpAuth.getSecret());
                return verifyTOTPCodeInternal(secret, code);
            })
            .orElse(false);
    }
    
    // =========================================================================
    // PRIVATE HELPER METHODS
    // =========================================================================
    
    /**
     * Generate random TOTP secret
     */
    private String generateTOTPSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[totpProperties.getSecretLength() / 8];
        random.nextBytes(bytes);
        return new Base32().encodeAsString(bytes).replaceAll("=", "");
    }
    
    /**
     * Generate TOTP URL for QR code
     */
    private String generateTOTPUrl(String secret, String accountName, String issuer) {
        return String.format(
            "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=%s&digits=%d&period=%d",
            issuer, accountName, secret, issuer,
            totpProperties.getAlgorithm(),
            totpProperties.getCodeDigits(),
            totpProperties.getTimeStep()
        );
    }
    
    /**
     * Generate QR code image as base64 data URL
     */
    private String generateQRCodeImage(String qrCodeUrl) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(
                qrCodeUrl, 
                BarcodeFormat.QR_CODE, 
                totpProperties.getQrCode().getSize(), 
                totpProperties.getQrCode().getSize()
            );
            
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, totpProperties.getQrCode().getFormat(), baos);
            
            byte[] imageBytes = baos.toByteArray();
            return String.format("data:image/%s;base64,%s", 
                totpProperties.getQrCode().getFormat().toLowerCase(),
                Base64.getEncoder().encodeToString(imageBytes)
            );
            
        } catch (Exception e) {
            log.error("Error generating QR code for TOTP setup", e);
            return null;
        }
    }
    
    /**
     * Verify TOTP code against secret with time skew tolerance
     */
    private boolean verifyTOTPCodeInternal(String secret, String code) {
        try {
            Duration timeStep = Duration.ofSeconds(totpProperties.getTimeStep());
            
            TimeBasedOneTimePasswordGenerator totp = new TimeBasedOneTimePasswordGenerator(
                timeStep, 
                totpProperties.getCodeDigits()
            );
            
            byte[] secretBytes = new Base32().decode(secret);
            SecretKeySpec key = new SecretKeySpec(secretBytes, "HmacSHA1");
            
            // Check current time and tolerance window
            long currentTime = System.currentTimeMillis() / 1000L;
            int tolerance = totpProperties.getTimeSkewTolerance();
            
            for (int i = -tolerance; i <= tolerance; i++) {
                long timeWindow = (currentTime / totpProperties.getTimeStep()) + i;
                Instant timeInstant = Instant.ofEpochSecond(timeWindow * totpProperties.getTimeStep());
                
                String expectedCode = String.format("%0" + totpProperties.getCodeDigits() + "d", 
                    totp.generateOneTimePassword(key, timeInstant));
                
                if (code.equals(expectedCode)) {
                    return true;
                }
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("Error verifying TOTP code", e);
            return false;
        }
    }
    
    /**
     * Disable existing primary 2FA method for user
     */
    private void disableExistingPrimaryMethod(User user) {
        twoFactorAuthRepository.findByUserAndIsPrimaryTrue(user)
            .ifPresent(existingPrimary -> {
                log.debug("Removing primary flag from existing method: {}", existingPrimary.getType());
                existingPrimary.setIsPrimary(false);
                twoFactorAuthRepository.save(existingPrimary);
            });
    }
}