package be.steby.CoreProject.bll.models;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Contexte de requête capturé pour une utilisation asynchrone.
 * DTO interne utilisé par les services de la couche business.
 */
@Getter
@Builder
@RequiredArgsConstructor
public class RequestContext {
    private final String clientIp;
    private final String userAgent;
    private final String sessionId;
    private final String requestId;
    private final CapturedHeaders headers;
    private final Long sessionDurationSeconds; // Durée de session calculée
    private final Long sessionCreationTime;    // Moment de création de la session

    @Getter
    @Builder
    public static class CapturedHeaders {
        private final String xForwardedFor;
        private final String acceptLanguage;
        private final String referer;
    }
}