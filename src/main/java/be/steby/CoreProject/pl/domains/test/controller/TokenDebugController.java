package be.steby.CoreProject.pl.domains.test.controller;

import be.steby.CoreProject.bll.common.services.tokens.SecureTokenService;
import be.steby.CoreProject.bll.domains.account.services.tokens.confirmation.AccountConfirmationTokenServiceImpl;
import be.steby.CoreProject.dal.repositories.tokens.AccountConfirmationTokenRepository;
import be.steby.CoreProject.dl.entities.tokens.AccountConfirmationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Temporary debug controller for token encryption issues.
 * Remove after debugging is complete.
 */
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@Slf4j
public class TokenDebugController {

    private final SecureTokenService secureTokenService;
    private final AccountConfirmationTokenServiceImpl accountConfirmationTokenService;
    private final AccountConfirmationTokenRepository accountConfirmationTokenRepository;

    @GetMapping("/token")
    public ResponseEntity<Map<String, Object>> debugToken(@RequestParam String token) {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("🔍 DEBUG - Starting token analysis");

            // 1. Token reçu
            result.put("received_token", token);
            log.info("🔍 Token reçu: {}", token);

            // 2. Est-ce un token sécurisé ?
            boolean isSecured = secureTokenService.isSecured(token);
            result.put("is_secured", isSecured);
            log.info("🔍 Is secured: {}", isSecured);

            // 3. Essayer de déchiffrer
            if (isSecured) {
                try {
                    String decrypted = secureTokenService.recoverToken(token);
                    result.put("decrypted_token", decrypted);
                    log.info("🔍 Token déchiffré: {}", decrypted);

                    // 4. Chercher le token déchiffré en base
                    Optional<AccountConfirmationToken> foundDecrypted = accountConfirmationTokenRepository.findByToken(decrypted);
                    result.put("found_with_decrypted", foundDecrypted.isPresent());
                    log.info("🔍 Token déchiffré trouvé en base: {}", foundDecrypted.isPresent());

                } catch (Exception decryptError) {
                    result.put("decryption_error", decryptError.getMessage());
                    log.error("🔍 Erreur déchiffrement: {}", decryptError.getMessage());
                }
            }

            // 5. Chercher le token original en base (sans déchiffrement)
            Optional<AccountConfirmationToken> foundOriginal = accountConfirmationTokenRepository.findByToken(token);
            result.put("found_with_original", foundOriginal.isPresent());
            log.info("🔍 Token original trouvé en base: {}", foundOriginal.isPresent());

            if (foundOriginal.isPresent()) {
                AccountConfirmationToken tokenEntity = foundOriginal.get();
                result.put("token_details", Map.of(
                        "id", tokenEntity.getId(),
                        "revoked", tokenEntity.isRevoked(),
                        "expired", tokenEntity.isExpired(),
                        "token_type", tokenEntity.getTokenType(),
                        "user_id", tokenEntity.getUser().getId()
                ));
            }

            // 6. Test avec le service
            try {
                AccountConfirmationToken serviceResult = accountConfirmationTokenService.getToken(token);
                result.put("service_success", true);
                result.put("service_token_id", serviceResult.getId());
                log.info("🔍 Service getToken réussi: {}", serviceResult.getId());
            } catch (Exception serviceError) {
                result.put("service_success", false);
                result.put("service_error", serviceError.getMessage());
                log.error("🔍 Service getToken échoué: {}", serviceError.getMessage());
            }

            result.put("status", "SUCCESS");

        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("error", e.getMessage());
            log.error("🔍 Erreur générale: {}", e.getMessage(), e);
        }

        return ResponseEntity.ok(result);
    }
}