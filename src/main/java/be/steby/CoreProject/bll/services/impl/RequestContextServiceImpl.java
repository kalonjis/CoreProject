package be.steby.CoreProject.bll.services.impl;

import be.steby.CoreProject.bll.models.RequestContext;
import be.steby.CoreProject.bll.services.RequestContextService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service pour extraire et capturer les informations de la requête HTTP
 * avant qu'elle ne soit recyclée par Spring.
 * Utilisé pour l'exécution asynchrone où l'accès direct à la requête n'est plus possible.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RequestContextServiceImpl implements RequestContextService {

    /**
     * Capture les informations essentielles de la requête HTTP
     */
    @Override
    public RequestContext captureRequestContext(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String sessionId = null;
        Long sessionCreationTime = null;
        Long sessionDurationSeconds = null;

        if (session != null) {
            sessionId = session.getId();
            sessionCreationTime = session.getCreationTime();
            // Calculer la durée de session au moment de la capture
            long now = System.currentTimeMillis();
            sessionDurationSeconds = (now - sessionCreationTime) / 1000;
        }

        return RequestContext.builder()
                .clientIp(extractClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .sessionId(sessionId)
                .requestId(request.getRequestId())
                .sessionCreationTime(sessionCreationTime)
                .sessionDurationSeconds(sessionDurationSeconds)
                .headers(RequestContext.CapturedHeaders.builder()
                        .xForwardedFor(request.getHeader("X-Forwarded-For"))
                        .acceptLanguage(request.getHeader("Accept-Language"))
                        .referer(request.getHeader("Referer"))
                        .build())
                .build();
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}