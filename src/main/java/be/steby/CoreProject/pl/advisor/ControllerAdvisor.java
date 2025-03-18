package be.steby.CoreProject.pl.advisor;

import be.steby.CoreProject.bll.exceptions.CoreProjectException;
import be.steby.CoreProject.bll.exceptions.TokenExpiredException;
import be.steby.CoreProject.dl.entities.tokens.AccountConfirmationToken;
import be.steby.CoreProject.dl.entities.tokens.BaseToken;
import be.steby.CoreProject.dl.entities.tokens.PasswordResetToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller advice class for global exception handling in the application.
 * Uses {@code @ControllerAdvice} to capture and handle specific exceptions in a centralized manner.
 */
@Slf4j
@ControllerAdvice
public class ControllerAdvisor {

    @Value("${url.back_server}")
    private String BACK_URL;

    /**
     * <p>Handles exceptions of type {@link CoreProjectException}.</p>
     * <p>This method captures {@link CoreProjectException}, logs the error message,
     * and returns an HTTP response with a status containing the error message.</p>
     *
     * @param error the {@link CoreProjectException} that was thrown
     * @return a {@link ResponseEntity} containing the error message with a status
     */
    @ExceptionHandler(CoreProjectException.class)
    public ResponseEntity<Map<String, String>> handleCoreProjectException(CoreProjectException error) {
        log.error("Exception occurred: {}", error.toString());
        HashMap<String, String> map = new HashMap<>();
        map.put("error", error.getMessage());
        return ResponseEntity.status(error.getStatus())
                .header("Content-Type", "application/json")
                .body(map);
    }

    /**
     * Handles method argument validation exceptions.
     * {@link MethodArgumentNotValidException}
     *
     * @param error The {@code MethodArgumentNotValidException} to handle.
     * @return A {@code ResponseEntity} with a status code of 406 (Not Acceptable) and a body containing a list of validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, List<String>>> handleValidationErrors(MethodArgumentNotValidException error) {
        Map<String, List<String>> errorResponse = new HashMap<>();
        errorResponse.put("errors", error.getBindingResult().getFieldErrors()
                .stream().map(FieldError::getDefaultMessage).collect(Collectors.toList()));
        errorResponse.get("errors").addAll(
                error.getBindingResult().getGlobalErrors()
                        .stream()
                        .map(DefaultMessageSourceResolvable::getDefaultMessage)
                        .toList()
        );
        errorResponse.put("globalErrors", error.getBindingResult().getGlobalErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.toList()));
        return ResponseEntity.status(406).body(errorResponse);
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleHttpMessageNotReadable(HttpMessageNotReadableException error) {
        log.error("Invalid request format: {}", error.getMessage());

        Map<String, String> response = new HashMap<>();
        response.put("error", "Invalid request format. Please check your JSON data.");

        return ResponseEntity.status(400) // Bad Request
                .header("Content-Type", "application/json")
                .body(response);
    }


    /**
     * Handles exceptions of type {@link TokenExpiredException} specifically for
     * PasswordResetToken and AccountConfirmationToken.
     *
     * @param error the {@link TokenExpiredException} that was thrown
     * @return a {@link ResponseEntity} with custom handling for specific token types
     */
    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<?> handleTokenExpiredException(TokenExpiredException error) {
        log.error("TokenExpiredException occurred: {}", error.toString());

        // On récupère le token depuis l'exception (il faudra modifier TokenExpiredException)
        if (error instanceof TokenExpiredException && error.getToken() != null) {
            BaseToken token = error.getToken();

            // Traitement spécifique pour PasswordResetToken ou AccountConfirmationToken
            if (token instanceof PasswordResetToken || token instanceof AccountConfirmationToken) {
                String email = token.getUser().getEmail();

                Map<String, String> response = new HashMap<>();
                String endpoint = token instanceof PasswordResetToken ?
                        "/api/password/request-password-token" : "/api/account/request-confirmation-token";
                String requestNewTokenUrl = BACK_URL + endpoint + "?token=" + token.getToken();

                String message = "Link has expired. Please restart procedure.";
                response.put("error", message);
                response.put("url", requestNewTokenUrl);

                return ResponseEntity.status(error.getStatus())
                        .header("Content-Type", "application/json")
                        .body(response);
            }
        }

        // Traitement par défaut pour les autres types de tokens
        HashMap<String, String> map = new HashMap<>();
        map.put("error", error.getMessage());
        return ResponseEntity.status(error.getStatus())
                .header("Content-Type", "application/json")
                .body(map);
    }
}

