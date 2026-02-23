package be.steby.CoreProject.pl.advisor;

import be.steby.CoreProject.bll.common.exceptions.SecurityLogException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Extension du ControllerAdvisor pour gérer spécifiquement les exceptions
 * liées à la journalisation de sécurité.
 *
 * Cet advisor a une priorité légèrement plus élevée que le ControllerAdvisor principal
 * pour s'assurer qu'il intercepte en premier les exceptions liées à la sécurité.
 */
@Slf4j
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 10) // Plus haute priorité que le ControllerAdvisor standard
public class SecurityLogControllerAdvisor {

    /**
     * Gère les exceptions de type SecurityLogException.
     * Ces exceptions sont spécifiques au module de journalisation de sécurité.
     *
     * @param error L'exception SecurityLogException qui a été levée
     * @return Une réponse HTTP avec un message d'erreur approprié
     */
    @ExceptionHandler(SecurityLogException.class)
    public ResponseEntity<Map<String, Object>> handleSecurityLogException(SecurityLogException error) {
        // Journaliser l'erreur avec plus de détails pour les administrateurs systèmes
        log.error("Erreur de journalisation de sécurité: {}", error.getMessage(), error);

        Map<String, Object> errorResponse = new HashMap<>();

        // Ajouter un message utilisateur convivial
        // En production, on pourrait ne pas révéler les détails exacts pour des raisons de sécurité
        errorResponse.put("message", "Une erreur est survenue lors de l'enregistrement de l'activité.");

        // Si l'application est en mode développement, on peut ajouter plus de détails
        if (isDevelopmentMode()) {
            errorResponse.put("error", error.getMessage());
            errorResponse.put("status", error.getStatus());

            // Ajouter la trace de la cause racine si disponible
            if (error.getCause() != null) {
                errorResponse.put("cause", error.getCause().getMessage());
            }
        }

        return ResponseEntity
                .status(error.getStatus())
                .body(errorResponse);
    }

    /**
     * Détermine si l'application est en mode développement.
     * Dans un vrai système, cela pourrait vérifier une configuration Spring.
     */
    private boolean isDevelopmentMode() {
        // Dans un vrai système, vous pourriez injecter l'environnement Spring et vérifier :
        // return Arrays.asList(env.getActiveProfiles()).contains("dev");

        // Pour cet exemple, on retourne simplement true
        return true;
    }
}